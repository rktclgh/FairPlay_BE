package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.entity.*;
import com.fairing.fairplay.banner.repository.*;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.S3UploadResponseDto;
import com.fairing.fairplay.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.fairing.fairplay.user.entity.Users;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fairing.fairplay.event.repository.EventRepository;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository bannerStatusCodeRepository;
    private final BannerActionCodeRepository bannerActionCodeRepository;
    private final BannerLogRepository bannerLogRepository;
    private final FileService fileService;
    private final BannerTypeRepository bannerTypeRepository;
    private final EventRepository eventRepository;

    // 배너 등록
    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto, Long adminId) {

        // 이벤트 존재 여부 검증
        if (dto.getEventId() == null || !eventRepository.existsById(dto.getEventId())) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null);
        }

        // 2) 파일 입력 검증: s3Key 또는 imageUrl 둘 중 하나는 있어야 함
        if (!StringUtils.hasText(dto.getS3Key()) && !StringUtils.hasText(dto.getImageUrl())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미지 정보가 없습니다. s3Key 또는 imageUrl이 필요합니다.", null);
        }

        // 3) 업로드/URL 처리
        String finalImageUrl;
        if (StringUtils.hasText(dto.getS3Key())) {
            S3UploadResponseDto uploadResult = fileService.uploadFile(
                    S3UploadRequestDto.builder()
                            .s3Key(dto.getS3Key())
                            .originalFileName(dto.getOriginalFileName())
                            .fileType(dto.getFileType())
                            .fileSize(dto.getFileSize())
                            .directoryPrefix("banner")
                            .build()
            );
            finalImageUrl = uploadResult.getFileUrl();
        } else {
            finalImageUrl = dto.getImageUrl();
        }

        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());
        BannerType bannerType = getBannerType(dto.getBannerTypeId());

        Banner banner = new Banner(
                dto.getTitle(),
                finalImageUrl,
                dto.getLinkUrl(),
                dto.getPriority(),
                dto.getStartDate(),
                dto.getEndDate(),
                statusCode,
                bannerType
        );
        banner.setEventId(dto.getEventId());
        banner.setCreatedBy(adminId);
        Banner saved = bannerRepository.save(banner);
        logBannerAction(saved, adminId, "CREATE");
        return toDto(saved);
    }

    // 배너 수정
    @Transactional
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());
        BannerType bannerType = getBannerType(dto.getBannerTypeId());

        if (dto.getEventId() != null && !eventRepository.existsById(dto.getEventId())) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null);
        }
        // 기간 검증(둘 다 들어왔을 때만)
        if (dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다.", null);
        }

        if (dto.getEventId() != null) {
            banner.setEventId(dto.getEventId());
        }


        // 이미지 최종 결정: 기본은 기존 값 유지
        String finalImageUrl = banner.getImageUrl();
/*
        // 새 이미지가 있을 경우 S3 재업로드
        if (dto.getS3Key() != null) {
            String directoryPrefix = "banner";
            S3UploadResponseDto uploadResult = fileService.uploadFile(
                    S3UploadRequestDto.builder()
                            .s3Key(dto.getS3Key())
                            .originalFileName(dto.getOriginalFileName())
                            .fileType(dto.getFileType())
                            .fileSize(dto.getFileSize())
                            .directoryPrefix(directoryPrefix)
                            .build()
            );
            banner.updateInfo(dto.getTitle(), finalImageUrl, dto.getLinkUrl(),
                    dto.getStartDate(), dto.getEndDate(), dto.getPriority(), bannerType, dto.isHot(), dto.isMdPick());
        } else {
            banner.updateInfo(dto.getTitle(), dto.getImageUrl(), dto.getLinkUrl(),
                    dto.getStartDate(), dto.getEndDate(), dto.getPriority(),bannerType, dto.isHot(), dto.isMdPick());
        }*/
        if (StringUtils.hasText(dto.getS3Key())) {
            // 새 이미지 S3 업로드 → 업로드 URL 사용
            S3UploadResponseDto uploadResult = fileService.uploadFile(
                    S3UploadRequestDto.builder()
                            .s3Key(dto.getS3Key())
                            .originalFileName(dto.getOriginalFileName())
                            .fileType(dto.getFileType())
                            .fileSize(dto.getFileSize())
                            .directoryPrefix("banner")
                            .build()
            );
            finalImageUrl = uploadResult.getFileUrl();
        } else if (StringUtils.hasText(dto.getImageUrl())) {
            // 외부 URL로 교체 의도일 때만 반영
            finalImageUrl = dto.getImageUrl();
        }
        // -------------------------------

        // 한 번에 업데이트
        banner.updateInfo(
                dto.getTitle(),
                finalImageUrl,
                dto.getLinkUrl(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getPriority(),
                bannerType
        );

        banner.updateStatus(statusCode);
        logBannerAction(banner, adminId, "UPDATE");

        return toDto(banner);
    }

    // 상태 전환
    @Transactional
    public void changeStatus(Long bannerId, BannerStatusUpdateDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        banner.updateStatus(statusCode);
        logBannerAction(banner, adminId, "UPDATE");
    }

    // 우선순위 변경
    @Transactional
    public void changePriority(Long bannerId, BannerPriorityUpdateDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        banner.updatePriority(dto.getPriority());

        logBannerAction(banner, adminId, "PRIORITY_CHANGE");
    }

    // 홈화면 배너 목록 조회
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        "ACTIVE", now, now
                )
                .stream()
                .map(this::toDto)
                .toList();
    }


    // 배너 목록 조회(조건 x, 모든)
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        return bannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void logBannerAction(Banner banner, Long adminId, String actionCodeStr) {
        BannerActionCode actionCode = bannerActionCodeRepository.findByCode(actionCodeStr)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 액션 코드: " + actionCodeStr));

        Users proxyUser = new Users(adminId);

        BannerLog log = BannerLog.builder()
                .banner(banner)
                .changedBy(proxyUser)
                .actionCode(actionCode)
                .build();

        bannerLogRepository.save(log);
    }

    private Banner getBanner(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 ID: " + id));
    }

    private BannerStatusCode getStatusCode(String code) {
        return bannerStatusCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 코드: " + code));
    }

    private BannerType getBannerType(Long id) { // 배너 타입 조회 메서드
        return bannerTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 타입 ID: " + id));
    }


    private BannerResponseDto toDto(Banner banner) {
        return BannerResponseDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .priority(banner.getPriority())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .statusCode(banner.getBannerStatusCode().getCode())
                .bannerTypeCode(banner.getBannerType().getCode())
                .build();
    }


}