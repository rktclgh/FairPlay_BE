package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.*;
import com.fairing.fairplay.booth.mapper.BoothApplicationMapper;
import com.fairing.fairplay.booth.repository.*;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.BoothEmailService;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.TempFileUploadDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.BoothAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import com.fairing.fairplay.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoothApplicationService {

    private final BoothApplicationRepository boothApplicationRepository;
    private final EventRepository eventRepository;
    private final BoothApplicationStatusCodeRepository statusCodeRepository;
    private final BoothPaymentStatusCodeRepository paymentCodeRepository;
    private final BoothApplicationMapper mapper;
    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final BoothRepository boothRepository;
    private final BoothTypeRepository boothTypeRepository;
    private final BoothAdminRepository boothAdminRepository;
    private final BoothExternalLinkRepository boothExternalLinkRepository;
    private final UserService userService;
    private final SuperAdminService superAdminService;
    private final PasswordEncoder passwordEncoder;
    private final AwsS3Service awsS3Service;
    private final FileService fileService;
    private final FileRepository fileRepository;
    private final BoothEmailService boothEmailService;
    private final NotificationService notificationService;


    // 부스 신청
    @Transactional
    public BoothApplicationResponseDto applyBooth(Long eventId, BoothApplicationRequestDto dto) {

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        if (userRepository.existsByEmail(dto.getBoothEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 등록된 이메일입니다.");
        }

        if (boothApplicationRepository.existsPendingApplicationByBoothEmail(dto.getBoothEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "해당 이메일로 이미 처리 대기 중인 신청이 존재합니다.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("행사를 찾을 수 없습니다."));

        BoothApplicationStatusCode status = statusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("상태 코드 없음"));

        BoothPaymentStatusCode paymentStatus = paymentCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드 없음"));

        BoothType boothType = boothTypeRepository.findById(dto.getBoothTypeId())
                .orElseThrow(() -> new EntityNotFoundException("선택한 부스 타입을 찾을 수 없습니다."));

        // 동시성 처리를 위한 부스 타입 생성된 부스 수 체크
        synchronized (this) {
            BoothType currentBoothType = boothTypeRepository.findById(dto.getBoothTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("선택한 부스 타입을 찾을 수 없습니다."));

            if (currentBoothType.getMaxApplicants() != null) {
                long existingBoothCount = boothRepository.countByBoothTypeAndIsDeletedFalse(currentBoothType);

                if (existingBoothCount >= currentBoothType.getMaxApplicants()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "해당 부스 타입의 생성 가능 부스 수가 초과되었습니다.");
                }
            }

            boothType = currentBoothType;
        }

//        BoothApplication boothApplication = mapper.toEntity(dto, event, boothType, status, paymentStatus);

        BoothApplication boothApplication = new BoothApplication();
        boothApplication.setEvent(event);
        boothApplication.setBoothEmail(dto.getBoothEmail());
        boothApplication.setBoothTitle(dto.getBoothTitle());
        boothApplication.setBoothDescription(dto.getBoothDescription());
        boothApplication.setStartDate(dto.getStartDate());
        boothApplication.setEndDate(dto.getEndDate());
        boothApplication.setBoothType(boothType);
        boothApplication.setManagerName(dto.getManagerName());
        boothApplication.setContactEmail(dto.getContactEmail());
        boothApplication.setContactNumber(dto.getContactNumber());
        boothApplication.setBoothApplicationStatusCode(status);
        boothApplication.setBoothPaymentStatusCode(paymentStatus);
        boothApplication.setApplyAt(LocalDateTime.now());

        // 먼저 BoothApplication 저장
        BoothApplication saved = boothApplicationRepository.save(boothApplication);

        // 저장된 BoothApplication으로 외부 링크 처리
        List<BoothExternalLinkDto> links = dto.getBoothExternalLinks() != null
                ? dto.getBoothExternalLinks()
                : List.of();
        for (BoothExternalLinkDto linkDto : links) {
            if (linkDto.getUrl() != null && !linkDto.getUrl().isBlank() &&
                    linkDto.getDisplayText() != null && !linkDto.getDisplayText().isBlank()) {
                BoothExternalLink link = new BoothExternalLink();
                link.setBoothApplication(saved);
                link.setUrl(linkDto.getUrl());
                link.setDisplayText(linkDto.getDisplayText());
                boothExternalLinkRepository.save(link);
                log.info("외부 링크 저장 완료: URL={}, DisplayText={}", linkDto.getUrl(), linkDto.getDisplayText());
            } else {
                log.warn("외부 링크 저장 건너뜀 - URL 또는 DisplayText가 비어있음: URL={}, DisplayText={}",
                        linkDto.getUrl(), linkDto.getDisplayText());
            }
        }

        if (dto.getTempBannerUrl() != null) {
            processAndLinkFiles(saved, dto.getTempBannerUrl());
        }

        List<BoothExternalLinkDto> externalLinkDtos = mapper.toBoothExternalLinkDto(saved);

        return BoothApplicationResponseDto.builder()
                .boothApplicationId(saved.getId())
                .boothTitle(saved.getBoothTitle())
                .boothDescription(saved.getBoothDescription())
                .boothEmail(saved.getBoothEmail())
                .managerName(saved.getManagerName())
                .contactEmail(saved.getContactEmail())
                .contactNumber(saved.getContactNumber())
                .boothTypeName(saved.getBoothType().getName())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .boothExternalLinks(externalLinkDtos)
                .statusCode(saved.getBoothApplicationStatusCode().getCode())
                .paymentStatus(saved.getBoothPaymentStatusCode().getCode())
                .applyAt(saved.getApplyAt())
                .build();
    }

    private void processAndLinkFiles(BoothApplication boothApply, TempFileUploadDto tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            String usage = tempFile.getUsage();
            if (usage == null || usage.isBlank()) {
                usage = "banner"; // 기본값
            }
            String directory = "booth-apply/" + usage;

            File savedFile = fileService.uploadFile(S3UploadRequestDto.builder()
                    .s3Key(tempFile.getS3Key())
                    .originalFileName(tempFile.getOriginalFileName())
                    .fileType(tempFile.getFileType())
                    .fileSize(tempFile.getFileSize())
                    .directoryPrefix(directory)
                    .usage(usage)
                    .build());

            fileService.createFileLink(savedFile, "BOOTH_APPLICATION", boothApply.getId());

            String cdnUrl = awsS3Service.getCdnUrl(savedFile.getFileUrl());
            boothApply.setBoothBannerUrl(cdnUrl);

        } catch (Exception e) {
            log.error("Error processing temporary file with key {}: {}", tempFile.getS3Key(), e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다: " + tempFile.getOriginalFileName());
        }
    }

    @Transactional(readOnly = true)
    public List<BoothApplicationListDto> getBoothApplications(Long eventId) {
        return boothApplicationRepository.findByEvent_EventId(eventId).stream()
                .map(mapper::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoothApplicationResponseDto getBoothApplication(Long id) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));
        return mapper.toResponseDto(application);
    }

    @Transactional
    public void updateStatus(Long id, BoothApplicationStatusUpdateDto dto) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));

        BoothApplicationStatusCode newStatus = statusCodeRepository.findByCode(dto.getStatusCode())
                .orElseThrow(() -> new EntityNotFoundException("상태 코드가 유효하지 않습니다."));

        application.setBoothApplicationStatusCode(newStatus);
        application.setAdminComment(dto.getAdminComment());
        application.setStatusUpdatedAt(LocalDateTime.now());

        if ("APPROVED".equals(newStatus.getCode())) {
            BoothType boothType = application.getBoothType();

            String tempPassword = userService.generateRandomPassword(12);
            BoothAdmin boothAdmin = createBoothAdminAccount(application, tempPassword);

            Booth booth = new Booth();
            booth.setEvent(application.getEvent());
            booth.setBoothTitle(application.getBoothTitle());
            booth.setBoothDescription(application.getBoothDescription());
            booth.setStartDate(application.getStartDate());
            booth.setEndDate(application.getEndDate());
            booth.setLocation("위치 미정");
            booth.setBoothType(boothType);
            booth.setBoothAdmin(boothAdmin);
            booth.setCreatedAt(LocalDateTime.now());

            Booth savedBooth = boothRepository.save(booth);
            // moveFilesToBooth에서 파일을 이동하고 배너 URL을 설정함
            moveFilesToBooth(application, savedBooth.getEvent().getEventId(), savedBooth.getId());

            application.updateStatus(newStatus, dto.getAdminComment());
            boothApplicationRepository.save(application);

            boothEmailService.sendApprovalEmail(
                    application.getContactEmail(),
                    application.getEvent().getTitleKr(),
                    application.getBoothTitle(),
                    application.getBoothEmail(),
                    tempPassword,
                    boothType.getName(),
                    boothType.getSize() != null ? boothType.getSize() : "미지정",
                    boothType.getPrice(),
                    application.getId()
            );

            try {
                NotificationRequestDto notificationDto = new NotificationRequestDto();
                notificationDto.setUserId(boothAdmin.getUserId());
                notificationDto.setTypeCode("BOOTH_APPROVAL");
                notificationDto.setMethodCode("WEB");
                notificationDto.setTitle("부스 등록 승인");
                notificationDto
                        .setMessage("'" + application.getBoothTitle() + "' 부스 등록 신청이 승인되었습니다. 관리자 계정으로 로그인하여 행사를 관리해보세요.");

                notificationService.createNotification(notificationDto);
            } catch (Exception e) {
                log.error("웹 알림 전송 실패 - BoothApply ID: {}, 오류: {}", id, e.getMessage());
            }

            log.info("부스 신청이 승인되었습니다. boothApplicationId: {}, boothId: {}", id, booth.getId());

        } else if ("REJECTED".equals(newStatus.getCode())) {
            application.updateStatus(newStatus, dto.getAdminComment());
            boothApplicationRepository.save(application);

            deleteBoothApplyFiles(application);

            boothEmailService.sendRejectionEmail(
                    application.getContactEmail(),
                    application.getEvent().getTitleKr(),
                    application.getBoothTitle(),
                    application.getAdminComment()
            );

            try {
                userRepository.findByEmail(application.getContactEmail()).ifPresent(user -> {
                    NotificationRequestDto notificationDto = new NotificationRequestDto();
                    notificationDto.setUserId(user.getUserId());
                    notificationDto.setTypeCode("BOOTH_REJECTION");
                    notificationDto.setMethodCode("WEB");
                    notificationDto.setTitle("부스 등록 반려");
                    notificationDto.setMessage("'" + application.getEvent().getTitleKr() + "에 신청하신" + application.getBoothTitle() + "' 부스 등록 신청이 반려되었습니다. 사유: " +
                            (application.getAdminComment() != null && !application.getAdminComment().trim().isEmpty() ? application.getAdminComment() : "관리자 검토 결과"));

                    try {
                        notificationService.createNotification(notificationDto);
                    } catch (Exception e) {
                        log.error("웹 알림 전송 실패 - BoothApply ID: {}, 오류: {}", id, e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("사용자 조회 실패 - BoothApply ID: {}, 오류: {}", id, e.getMessage());
            }

            log.info("부스 신청이 반려되었습니다. BoothApply Id: {}", id);
        }
    }

    private BoothAdmin createBoothAdminAccount(BoothApplication boothApply, String tempPassword) {
        log.info("사용자 계정 생성 시작 - 이메일: {}, 이름: {}", boothApply.getBoothEmail(), boothApply.getManagerName());

        // 이메일 중복 체크 (혹시 다른 곳에서 같은 이메일이 생성되었을 경우를 대비)
        if (userRepository.existsByEmail(boothApply.getBoothEmail())) {
            log.error("이미 존재하는 이메일로 사용자 생성 시도: {}", boothApply.getBoothEmail());
            throw new CustomException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다: " + boothApply.getBoothEmail());
        }

        UserRoleCode boothAdminRole = userRoleCodeRepository.findByCode("BOOTH_MANAGER")
                .orElseThrow(() -> {
                    log.error("BOOTH_MANAGER 역할을 찾을 수 없음. 사용 가능한 역할을 확인하세요.");
                    return new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "BOOTH_MANAGER 역할을 찾을 수 없습니다.");
                });

        log.info("BOOTH_MANAGER 역할 찾기 성공 - 역할 ID: {}", boothAdminRole.getId());

        Users user = Users.builder()
                .email(boothApply.getBoothEmail())
                .password(passwordEncoder.encode(tempPassword))
                .name(boothApply.getManagerName())
                .nickname(boothApply.getManagerName())
                .phone(boothApply.getContactNumber())
                .roleCode(boothAdminRole)
                .build();

        log.info("사용자 엔티티 빌드 완료, 저장 시도 중");
        Users savedUser = userRepository.save(user);
        log.info("사용자 저장 성공 - 사용자 ID: {}", savedUser.getUserId());

        superAdminService.setBoothAdmin(savedUser.getUserId());

        BoothAdmin boothAdmin = new BoothAdmin();
        boothAdmin.setUser(savedUser);
        boothAdmin.setEmail(boothApply.getContactEmail());
        boothAdmin.setManagerName(boothApply.getManagerName());
        boothAdmin.setContactNumber(boothApply.getContactNumber());

        log.info("BoothAdmin 엔티티 생성 완료, 저장 시도 중");
        BoothAdmin savedBoothAdmin = boothAdminRepository.save(boothAdmin);
        log.info("BoothAdmin 저장 성공 - BoothAdmin ID: {}", savedBoothAdmin.getUserId());

        return savedBoothAdmin;
    }

    private String determineFileUsage(File file) {
        String directory = file.getDirectory();
        if (directory != null) {
            if (directory.contains("banner")) {
                return "banners";
            } else if (directory.contains("thumbnail")) {
                return "thumbnails";
            }
        }

        return "documents";
    }

    private void moveFilesToBooth(BoothApplication boothApply, Long eventId, Long boothId) {
        try {
            Booth booth = boothRepository.findById(boothId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당하는 부스를 찾을 수 없습니다."));
            List<File> boothApplyFile = fileRepository.findByTargetTypeAndTargetId("BOOTH_APPLICATION", boothApply.getId());

            log.info("부스 파일 이동 시작 - BoothApplication ID: {}, 찾은 파일 수: {}", boothApply.getId(), boothApplyFile.size());

            for (File file : boothApplyFile) {
                try {
                    String usage = determineFileUsage(file);
                    log.info("파일 처리 - FileId: {}, Directory: {}, Usage: {}", file.getId(), file.getDirectory(), usage);

                    String newCdnUrl = fileService.moveFileToBooth(file.getFileUrl(), eventId, boothId, usage);

                    // URL 업데이트
                    switch (usage) {
                        case "banners":
                        case "banner":
                            booth.setBoothBannerUrl(newCdnUrl);
                            log.info("배너 URL 업데이트 - BoothId: {}, URL: {}", boothId, newCdnUrl);
                            break;
                    }

                    log.info("파일 이동 성공 - EventApply: {}, File: {}, Usage: {}, NewURL: {}",
                            boothApply.getId(), file.getId(), usage, newCdnUrl);

                } catch (Exception e) {
                    log.error("개별 파일 이동 실패 - EventApply: {}, File: {}, 오류: {}",
                            boothApply.getId(), file.getId(), e.getMessage());
                }
            }

            boothRepository.save(booth);
            log.info("파일 이동 완료 - BoothId: {}, BoothApply ID: {}", boothId, boothApply.getId());

        } catch (Exception e) {
            log.error("파일 이동 중 오류 발생 - EventApply ID: {}, 오류: {}",
                    boothApply.getId(), e.getMessage());
        }
    }

    private void deleteBoothApplyFiles(BoothApplication boothApply) {
        try {
            List<File> filesToDelete = fileRepository.findByTargetTypeAndTargetId("BOOTH_APPLICATION", boothApply.getId());
            for (File file : filesToDelete) {
                fileService.deleteFile(file.getId());
            }
            log.info("BoothApplication 파일 삭제 완료 - BoothApplication ID: {}", boothApply.getId());
        } catch (Exception e) {
            log.error("BoothApplication 파일 삭제 중 오류 발생 - BoothApplication ID: {}, 오류: {}",
                    boothApply.getId(), e.getMessage());
        }
    }

    @Transactional
    public void updatePaymentStatus(Long id, BoothPaymentStatusUpdateDto dto) {
        BoothApplication booth = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        BoothPaymentStatusCode statusCode = paymentCodeRepository
                .findByCode(dto.getPaymentStatusCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제 상태 코드입니다."));

        booth.setBoothPaymentStatusCode(statusCode);
        booth.setAdminComment(dto.getAdminComment());
        booth.setStatusUpdatedAt(LocalDateTime.now());

        if ("PAID".equals(dto.getPaymentStatusCode())) {
            Users user = userRepository.findByEmail(booth.getContactEmail())
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

            UserRoleCode boothManagerCode = userRoleCodeRepository.findByCode("BOOTH_MANAGER")
                    .orElseThrow(() -> new EntityNotFoundException("BOOTH_MANAGER 권한 코드가 존재하지 않습니다."));

            user.setRoleCode(boothManagerCode);
        }

    }

    @Transactional
    public void cancelApplication(Long id, Long userId) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다."));
        String requesterEmail = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getEmail();

        if (!application.getContactEmail().equalsIgnoreCase(requesterEmail)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "해당 신청을 취소할 권한이 없습니다.");
        }

        if (!"APPROVED".equals(application.getBoothApplicationStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "승인되지 않은 부스 신청입니다.");
        }
        if ("PAID".equals(application.getBoothPaymentStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 결제가 완료된 부스입니다.");
        }

        BoothPaymentStatusCode cancelled = paymentCodeRepository.findByCode("CANCELLED")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드(CANCELLED)를 찾을 수 없습니다."));

        application.setBoothPaymentStatusCode(cancelled);
        application.setStatusUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BoothPaymentPageDto getBoothPaymentInfo(Long applicationId) {
        BoothApplication application = boothApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        return BoothPaymentPageDto.builder()
                .applicationId(application.getId())
                .eventTitle(application.getEvent().getTitleKr())
                .boothTitle(application.getBoothTitle())
                .boothTypeName(application.getBoothType().getName())
                .boothTypeSize(application.getBoothType().getSize())
                .price(application.getBoothType().getPrice())
                .managerName(application.getManagerName())
                .contactEmail(application.getContactEmail())
                .paymentStatus(application.getBoothPaymentStatusCode().getCode())
                .build();
    }
}