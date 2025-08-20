package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.entity.BoothExternalLink;
import com.fairing.fairplay.booth.entity.BoothType;
import com.fairing.fairplay.booth.mapper.BoothApplicationMapper;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothExternalLinkRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.booth.repository.BoothTypeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.dto.BoothAdminRequestDto;
import com.fairing.fairplay.user.dto.BoothAdminResponseDto;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.BoothAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoothService {

    private final BoothRepository boothRepository;
    private final EventRepository eventRepository;
    private final BoothTypeRepository boothTypeRepository;
    private final BoothApplicationRepository boothApplicationRepository;
    private final FileService fileService;
    private final AwsS3Service awsS3Service;
    private final BoothExternalLinkRepository boothExternalLinkRepository;
    private final BoothAdminRepository boothAdminRepository;
    private final UserRepository userRepository;
    private final BoothApplicationMapper boothApplicationMapper;

    // 부스 목록 조회 (관리자용 - 삭제된 부스 포함 조회)
    @Transactional(readOnly = true)
    public List<BoothSummaryForManagerResponseDto> getAllBooths(Long eventId) {
        List<Booth> booths = boothRepository.findAllByEvent(getEvent(eventId));
        List<BoothSummaryForManagerResponseDto> boothsDto = new ArrayList<>();
        booths.forEach(booth -> boothsDto.add(BoothSummaryForManagerResponseDto.from(booth)));
        return boothsDto;
    }

    // 삭제된 부스 제외하고 결제 완료된 부스 목록 조회
    @Transactional(readOnly = true)
    public List<BoothSummaryResponseDto> getBooths(Long eventId) {
        Event event = getEvent(eventId);

        // 1. 이벤트 ID로 모든 부스 신청서를 가져옵니다.
        List<BoothApplication> applications = boothApplicationRepository.findByEvent_EventId(eventId);

        // 2. 결제가 완료된 신청서들의 이메일을 수집합니다.
        Set<String> paidBoothEmails = applications.stream()
                .filter(app -> app.getBoothPaymentStatusCode() != null && "PAID".equals(app.getBoothPaymentStatusCode().getCode()))
                .map(BoothApplication::getBoothEmail)
                .collect(Collectors.toSet());

        // 3. 삭제되지 않은 모든 부스를 가져옵니다.
        List<Booth> booths = boothRepository.findByEventAndIsDeletedFalse(event);
        log.info("booths: {}", booths);

        // 4. 부스 관리자의 이메일이 결제 완료된 신청서의 이메일과 일치하는 부스만 필터링합니다.
        List<Booth> paidBooths = booths.stream()
                .filter(booth -> {
                    if (booth.getBoothAdmin() != null && booth.getBoothAdmin().getUser() != null) {
                        String boothAdminEmail = booth.getBoothAdmin().getUser().getEmail();
                        return paidBoothEmails.contains(boothAdminEmail);
                    }
                    return false;
                })
                .toList();
        log.info("paidBooths: {}", paidBooths);

        List<BoothSummaryResponseDto> boothsDto = new ArrayList<>();
        paidBooths.forEach(booth -> boothsDto.add(BoothSummaryResponseDto.from(booth)));

        return boothsDto;
    }

    // 부스 상세 조회
    @Transactional(readOnly = true)
    public BoothDetailResponseDto getBoothDetails(Long boothId) {
        Booth booth = getBooth(boothId);
        List<BoothExternalLinkDto> externalLinkDtos = boothExternalLinkRepository.findByBooth(booth).stream()
                .map(link -> BoothExternalLinkDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build()).toList();

        return BoothDetailResponseDto.builder()
                .boothId(booth.getId())
                .boothTitle(booth.getBoothTitle())
                .boothBannerUrl(booth.getBoothBannerUrl())
                .boothDescription(booth.getBoothDescription())
                .boothTypeName(booth.getBoothType().getName())
                .location(booth.getLocation())
                .startDate(booth.getStartDate())
                .endDate(booth.getEndDate())
                .managerName(booth.getBoothAdmin().getManagerName())
                .contactEmail(booth.getBoothAdmin().getEmail())
                .contactNumber(booth.getBoothAdmin().getContactNumber())
                .boothExternalLinks(externalLinkDtos)
                .build();
    }

    // 부스 정보 업데이트
    @Transactional
    public BoothDetailResponseDto updateBooth(Long boothId, BoothUpdateRequestDto dto) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new EntityNotFoundException("부스를 찾을 수 없습니다: " + boothId));

        // Update booth fields
        if (dto.getBoothTitle() != null) {
            booth.setBoothTitle(dto.getBoothTitle());
        }

        if (dto.getBoothDescription() != null) {
            booth.setBoothDescription(dto.getBoothDescription());
        }

        if (dto.getStartDate() != null) {
            booth.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            booth.setEndDate(dto.getEndDate());
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        if (dto.getLocation() != null) {
            booth.setLocation(dto.getLocation());
        }

        if (dto.getBoothExternalLinks() != null) {
            // 기존 외부 링크 삭제
            boothExternalLinkRepository.deleteByBooth(booth);

            // 새로운 외부 링크 저장
            for (BoothExternalLinkDto linkDto : dto.getBoothExternalLinks()) {
                if (linkDto.getUrl() != null && !linkDto.getUrl().isEmpty() &&
                        linkDto.getDisplayText() != null && !linkDto.getDisplayText().isEmpty()) {
                    BoothExternalLink link = new BoothExternalLink();
                    link.setBooth(booth);
                    link.setUrl(linkDto.getUrl());
                    link.setDisplayText(linkDto.getDisplayText());
                    boothExternalLinkRepository.save(link);
                    log.info("외부 링크 저장 완료: URL={}, DisplayText={}", linkDto.getUrl(), linkDto.getDisplayText());
                } else {
                    log.warn("외부 링크 저장 건너뜀 - URL 또는 DisplayText가 비어있음: URL={}, DisplayText={}",
                            linkDto.getUrl(), linkDto.getDisplayText());
                }
            }
        }

        // Handle file deletions
        if (dto.getDeletedFileIds() != null && !dto.getDeletedFileIds().isEmpty()) {
            for (Long fileId : dto.getDeletedFileIds()) {
                fileService.deleteFile(fileId);
            }
        }

        // Handle file uploads
        if (dto.getTempFiles() != null && !dto.getTempFiles().isEmpty()) {
            dto.getTempFiles().forEach(tempFile -> {
                String usage = tempFile.getUsage();
                if (usage == null || usage.isBlank()) {
                    usage = "banner"; // 기본값
                }

                String directory = "booth/" + boothId + "/" + usage;
                File savedFile = fileService.uploadFile(S3UploadRequestDto.builder()
                        .s3Key(tempFile.getS3Key())
                        .originalFileName(tempFile.getOriginalFileName())
                        .fileType(tempFile.getFileType())
                        .fileSize(tempFile.getFileSize())
                        .directoryPrefix(directory)
                        .usage(usage)
                        .build());

                fileService.createFileLink(savedFile, "BOOTH", boothId);

                // 배너 파일인 경우 boothBannerUrl 업데이트
                if ("banner".equals(usage)) {
                    String cdnUrl = awsS3Service.getCdnUrl(savedFile.getFileUrl());
                    booth.setBoothBannerUrl(cdnUrl);
                    log.info("부스 배너 URL 업데이트: {}", cdnUrl);
                }
            });
        }

        boothRepository.saveAndFlush(booth);

        List<BoothExternalLinkDto> externalLinkDtos = boothExternalLinkRepository.findByBooth(booth).stream()
                .map(link -> BoothExternalLinkDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build()).toList();

        return BoothDetailResponseDto.builder()
                .boothId(booth.getId())
                .boothTitle(booth.getBoothTitle())
                .boothBannerUrl(booth.getBoothBannerUrl())
                .boothDescription(booth.getBoothDescription())
                .boothTypeName(booth.getBoothType().getName())
                .location(booth.getLocation())
                .startDate(booth.getStartDate())
                .endDate(booth.getEndDate())
                .managerName(booth.getBoothAdmin().getManagerName())
                .contactEmail(booth.getBoothAdmin().getEmail())
                .contactNumber(booth.getBoothAdmin().getContactNumber())
                .boothExternalLinks(externalLinkDtos)
                .build();
    }

    @Transactional
    public void deleteBooth(Long boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스를 찾을 수 없습니다."));
        booth.setIsDeleted(true);
        boothRepository.save(booth);
    }

    // 부스 관리자 정보 변경
    @Transactional
    public BoothAdminResponseDto updateBoothAdmin(Long boothId, BoothAdminRequestDto dto) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스를 찾을 수 없습니다."));
        BoothAdmin boothAdmin = booth.getBoothAdmin();

        if (dto.getManagerName() != null) {
            boothAdmin.setManagerName(dto.getManagerName());
        }

        if (dto.getContactEmail() != null) {
            boothAdmin.setEmail(dto.getContactEmail());
        }

        if (dto.getContactNumber() != null) {
            boothAdmin.setContactNumber(dto.getContactNumber());
        }

        boothAdminRepository.saveAndFlush(boothAdmin);

        return BoothAdminResponseDto.builder()
                .boothId(booth.getId())
                .contactNumber(boothAdmin.getContactNumber())
                .contactEmail(boothAdmin.getEmail())
                .build();
    }


    /********************** 부스 타입 관리 **********************/

    // 부스 타입 생성
    @Transactional
    public BoothTypeDto createBoothType(Long eventId, BoothTypeDto dto) {
        BoothType boothType = new BoothType();
        boothType.setName(dto.getName());
        boothType.setSize(dto.getSize());
        boothType.setPrice(dto.getPrice());
        boothType.setMaxApplicants(dto.getMaxApplicants());
        boothType.setEvent(getEvent(eventId));

        boothTypeRepository.saveAndFlush(boothType);

        return BoothTypeDto.builder()
                .id(boothType.getId())
                .price(boothType.getPrice())
                .size(boothType.getSize())
                .name(boothType.getName())
                .maxApplicants(boothType.getMaxApplicants())
                .build();
    }

    // 부스 타입 목록 조회
    @Transactional(readOnly = true)
    public List<BoothTypeDto> getBoothTypes(Long eventId) {
        List<BoothType> boothTypes = boothTypeRepository.findAllByEvent(getEvent(eventId));
        return boothTypes.stream()
                .map(boothType -> BoothTypeDto.builder()
                        .id(boothType.getId())
                        .price(boothType.getPrice())
                        .size(boothType.getSize())
                        .name(boothType.getName())
                        .maxApplicants(boothType.getMaxApplicants())
                        .build())
                .toList();
    }

    // 부스 타입 수정
    @Transactional
    public BoothTypeDto updateBoothType(Long boothTypeId, BoothTypeDto dto) {
        BoothType boothType = getBoothType(boothTypeId);
        boothType.setName(dto.getName());
        boothType.setSize(dto.getSize());
        boothType.setPrice(dto.getPrice());
        boothType.setMaxApplicants(dto.getMaxApplicants());

        boothTypeRepository.saveAndFlush(boothType);

        return BoothTypeDto.builder()
                .id(boothType.getId())
                .price(boothType.getPrice())
                .size(boothType.getSize())
                .name(boothType.getName())
                .maxApplicants(boothType.getMaxApplicants())
                .build();
    }

    // 부스 타입 삭제
    @Transactional
    public void deleteBoothType(Long boothTypeId) {
        boothTypeRepository.deleteById(boothTypeId);
    }

    /********************** 부스 관리자 기능 **********************/
    // 부스 관리자 - 내 부스 목록 조회 (실제 생성된 부스)
    @Transactional(readOnly = true)
    public List<BoothAdminDashboardDto> getMyBooths(Long userId) {
        // 1. 현재 로그인한 사용자의 정보를 가져옵니다.
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        String userEmail = user.getEmail();

// 2. 현재 사용자가 관리하는 모든 부스를 조회합니다. (효율적인 쿼리 사용)
// BoothRepository에 findByBoothAdmin_User_Id(userId) 같은 메소드가 필요합니다.
        List<Booth> managedBooths = boothRepository.findByBoothAdmin_UserId(userId);

// 관리하는 부스가 없으면 빈 리스트를 반환하고 종료합니다.
        if (managedBooths.isEmpty()) {
            return Collections.emptyList();
        }

// 3. 사용자가 관리하는 부스들을 행사(Event) ID를 기준으로 그룹화합니다.
        Map<Long, List<Booth>> boothsByEventId = managedBooths.stream()
                .collect(Collectors.groupingBy(booth -> booth.getEvent().getEventId()));

// 최종 결과를 담을 리스트를 생성합니다.
        List<BoothAdminDashboardDto> resultDtos = new ArrayList<>();

// 4. 각 행사 ID 별로 반복하며 신청 정보를 찾고 DTO를 생성합니다.
        for (Map.Entry<Long, List<Booth>> entry : boothsByEventId.entrySet()) {
            Long eventId = entry.getKey();
            List<Booth> boothsForThisEvent = entry.getValue();

            // '현재 행사 ID'와 '사용자 이메일'로 신청서를 조회합니다.
            Optional<BoothApplication> applicationOpt =
                    boothApplicationRepository.findByEvent_EventIdAndBoothEmail(eventId, userEmail);

            // 만약 해당 행사에 대한 사용자의 신청서가 존재한다면,
            if (applicationOpt.isPresent()) {
                BoothApplication application = applicationOpt.get();
                String paymentStatusCode = application.getBoothPaymentStatusCode().getCode();
                String paymentStatusName = application.getBoothPaymentStatusCode().getName();

                // 이 행사에 속한 모든 관리 부스에 대해 DTO를 생성합니다.
                List<BoothAdminDashboardDto> dtosForThisEvent = boothsForThisEvent.stream()
                        .map(booth -> BoothAdminDashboardDto.from(booth, paymentStatusCode, paymentStatusName))
                        .collect(Collectors.toList());

                resultDtos.addAll(dtosForThisEvent);
            }
        }

        return resultDtos;
    }

    /********************** 헬퍼 메소드 **********************/
    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."));
    }

    private Booth getBooth(Long boothId) {
        return boothRepository.findById(boothId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스를 찾을 수 없습니다."));
    }

    private BoothType getBoothType(Long boothTypeId) {
        return boothTypeRepository.findById(boothTypeId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스 타입을 찾을 수 없습니다."));
    }

}
