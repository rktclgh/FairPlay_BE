package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.entity.*;
import com.fairing.fairplay.banner.repository.*;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_PRIORITY_CHANGE = "PRIORITY_CHANGE";
    private static final String TYPE_MD_PICK = "MD_PICK";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository bannerStatusCodeRepository;
    private final BannerActionCodeRepository bannerActionCodeRepository;
    private final BannerLogRepository bannerLogRepository;
    private final FileService fileService;
    private final AwsS3Service awsS3Service;
    private final BannerTypeRepository bannerTypeRepository;
    private final EventRepository eventRepository;

    @Value("${cloud.aws.s3.banner-dir:banner}")
    private String bannerDir;

    // 등록
    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto, Long adminId) {
        validateEvent(dto.getEventId());


        if (dto.getStartDate() == null || dto.getEndDate() == null) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간(startDate, endDate)은 필수입니다.", null);
                    }
                if (dto.getStartDate().isAfter(dto.getEndDate())) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다.", null);
                    }

        BannerStatusCode statusCode = getStatusCodeOr404(dto.getStatusCode());
        BannerType bannerType = getBannerTypeOr404(dto.getBannerTypeId());

        Banner banner = new Banner(
                dto.getTitle(),
                null, // 이미지 URL은 파일 처리 후 설정
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

        if (TYPE_MD_PICK.equals(bannerType.getCode()) && STATUS_ACTIVE.equals(statusCode.getCode())) {
            BannerStatusCode inactive = getStatusCodeOr404(STATUS_INACTIVE);
            bannerRepository.deactivateOthersActiveByType(TYPE_MD_PICK, STATUS_ACTIVE, inactive, saved.getId());
        }

        String finalImageUrl = resolveImageUrlForCreate(dto, saved.getId());
        saved.setImageUrl(finalImageUrl);

        logBannerAction(saved, adminId, ACTION_CREATE);
        return toDto(saved);
    }

    // 수정
    @Transactional
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);

        if (dto.getEventId() != null) {
            validateEvent(dto.getEventId());
            banner.setEventId(dto.getEventId());
        }

        // 부분 수정 시 기존 값과 병합하여 기간 검증
                LocalDateTime newStart = dto.getStartDate() != null ? dto.getStartDate() : banner.getStartDate();
                LocalDateTime newEnd   = dto.getEndDate()   != null ? dto.getEndDate()   : banner.getEndDate();
                if (newStart.isAfter(newEnd)) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다.", null);
                    }

        // 통일된 이미지 처리(수정 전용: 입력 없으면 기존 유지)
        String finalImageUrl = resolveImageUrlForUpdate(dto, banner, adminId);

        BannerStatusCode statusCode = getStatusCodeOr404(dto.getStatusCode());
        BannerType bannerType = getBannerTypeOr404(dto.getBannerTypeId());

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

// MD_PICK 하나만 유지: 결과가 MD_PICK + ACTIVE면 자기 자신 제외 모두 INACTIVE
        if (TYPE_MD_PICK.equals(banner.getBannerType().getCode())
                && STATUS_ACTIVE.equals(banner.getBannerStatusCode().getCode())) {
            BannerStatusCode inactive = getStatusCodeOr404(STATUS_INACTIVE);
            bannerRepository.deactivateOthersActiveByType(
                    TYPE_MD_PICK, STATUS_ACTIVE, inactive, banner.getId()
            );
        }

        logBannerAction(banner, adminId, ACTION_UPDATE);
        return toDto(banner);
    }

    // 상태 우선 순위
    @Transactional
    public void changeStatus(Long bannerId, BannerStatusUpdateDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);
        BannerStatusCode statusCode = getStatusCodeOr404(dto.getStatusCode());
        banner.updateStatus(statusCode);
        logBannerAction(banner, adminId, ACTION_UPDATE);

        //  MD_PICK을 ACTIVE로 바꾸면, 자신 제외 모두 INACTIVE
        if (TYPE_MD_PICK.equals(banner.getBannerType().getCode())
                && STATUS_ACTIVE.equals(statusCode.getCode())) {
            BannerStatusCode inactive = getStatusCodeOr404(STATUS_INACTIVE);
            bannerRepository.deactivateOthersActiveByType(
                    TYPE_MD_PICK, STATUS_ACTIVE, inactive, banner.getId()
            );
        }
    }

    @Transactional
    public void changePriority(Long bannerId, BannerPriorityUpdateDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);
        banner.updatePriority(dto.getPriority());
        logBannerAction(banner, adminId, ACTION_PRIORITY_CHANGE);
    }

    // 조회
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        STATUS_ACTIVE, now, now
                )
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 공통 핼퍼

    private void validateEvent(Long eventId) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null);
        }
    }

    // 등록: s3Key 또는 imageUrl 반드시 필요(없으면 400)
    private String resolveImageUrlForCreate(BannerRequestDto dto, Long bannerId) {
        if (!StringUtils.hasText(dto.getS3Key()) && !StringUtils.hasText(dto.getImageUrl())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미지 정보가 없습니다. s3Key 또는 imageUrl이 필요합니다.", null);
        }
        if (StringUtils.hasText(dto.getS3Key())) {
            return uploadToS3(dto, bannerId);
        }
        return dto.getImageUrl();
    }

    // 수정: s3Key가 있으면 업로드, imageUrl이 있으면 교체, 둘 다 없으면 기존 유지
    private String resolveImageUrlForUpdate(BannerRequestDto dto, Banner banner, Long adminId) {
        if (StringUtils.hasText(dto.getS3Key())) {
            // 기존 파일이 있다면 삭제
            if (StringUtils.hasText(banner.getImageUrl())) {
                try {
                    String s3Key = awsS3Service.getS3KeyFromPublicUrl(banner.getImageUrl());
                    if (s3Key != null) {
                        fileService.deleteFileByS3Key(s3Key);
                    }
                } catch (Exception e) {
                    log.warn("기존 배너 이미지 S3 삭제 실패 - URL: {}, Error: {}", banner.getImageUrl(), e.getMessage());
                }
            }
            return uploadToS3(dto, banner.getId());
        }
        if (StringUtils.hasText(dto.getImageUrl())) {
            return dto.getImageUrl();
        }
        return banner.getImageUrl();
    }

    private String uploadToS3(BannerRequestDto dto, Long bannerId) {
        File savedFile = fileService.uploadFile(
                S3UploadRequestDto.builder()
                        .s3Key(dto.getS3Key())
                        .originalFileName(dto.getOriginalFileName())
                        .fileType(dto.getFileType())
                        .fileSize(dto.getFileSize())
                        .directoryPrefix(bannerDir)
                        .usage("banner")
                        .build()
        );
        fileService.createFileLink(savedFile, "BANNER", bannerId);
        return awsS3Service.getCdnUrl(savedFile.getFileUrl());
    }

    private void logBannerAction(Banner banner, Long adminId, String actionCodeStr) {
        BannerActionCode actionCode = bannerActionCodeRepository.findByCode(actionCodeStr)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 액션 코드: " + actionCodeStr, null));

        Users proxyUser = new Users(adminId);
        BannerLog log = BannerLog.builder()
                .banner(banner)
                .changedBy(proxyUser)
                .actionCode(actionCode)
                .build();

        bannerLogRepository.save(log);
    }

    private Banner getBannerOr404(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 ID: " + id, null));
    }

    private BannerStatusCode getStatusCodeOr404(String code) {
        return bannerStatusCodeRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 상태 코드: " + code, null));
    }

    private BannerType getBannerTypeOr404(Long id) {
        return bannerTypeRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 타입 ID: " + id, null));
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