package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.EventEmailService;
import com.fairing.fairplay.core.email.service.TemporaryPasswordEmailService;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.dto.EventApplyRequestDto;
import com.fairing.fairplay.event.dto.EventApplyResponseDto;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.event.repository.RegionCodeRepository;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import com.fairing.fairplay.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventApplyService {

    private final EventApplyRepository eventApplyRepository;
    private final ApplyStatusCodeRepository applyStatusCodeRepository;
    private final LocationRepository locationRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final EventStatusCodeRepository eventStatusCodeRepository;
    private final EventVersionService eventVersionService;
    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final EventAdminRepository eventAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final Hashids hashids;
    private final TemporaryPasswordEmailService temporaryPasswordEmailService; // Keep for now, will remove helper
    private final AwsS3Service awsS3Service;
    private final UserService userService;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final EventEmailService eventEmailService;
    private final NotificationService notificationService;
    private final RegionCodeRepository regionCodeRepository;

    public EventApplyService(EventApplyRepository eventApplyRepository, ApplyStatusCodeRepository applyStatusCodeRepository, LocationRepository locationRepository, MainCategoryRepository mainCategoryRepository, SubCategoryRepository subCategoryRepository, EventRepository eventRepository, EventDetailRepository eventDetailRepository, EventStatusCodeRepository eventStatusCodeRepository, EventVersionService eventVersionService, UserRepository userRepository, UserRoleCodeRepository userRoleCodeRepository, EventAdminRepository eventAdminRepository, PasswordEncoder passwordEncoder, Hashids hashids, TemporaryPasswordEmailService temporaryPasswordEmailService, AwsS3Service awsS3Service, UserService userService, FileRepository fileRepository, FileService fileService, EventEmailService eventEmailService, NotificationService notificationService, RegionCodeRepository regionCodeRepository) {
        this.eventApplyRepository = eventApplyRepository;
        this.applyStatusCodeRepository = applyStatusCodeRepository;
        this.locationRepository = locationRepository;
        this.mainCategoryRepository = mainCategoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.eventRepository = eventRepository;
        this.eventDetailRepository = eventDetailRepository;
        this.eventStatusCodeRepository = eventStatusCodeRepository;
        this.eventVersionService = eventVersionService;
        this.userRepository = userRepository;
        this.userRoleCodeRepository = userRoleCodeRepository;
        this.eventAdminRepository = eventAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.hashids = hashids;
        this.temporaryPasswordEmailService = temporaryPasswordEmailService;
        this.awsS3Service = awsS3Service;
        this.userService = userService;
        this.fileRepository = fileRepository;
        this.fileService = fileService;
        this.eventEmailService = eventEmailService;
        this.notificationService = notificationService;
        this.regionCodeRepository = regionCodeRepository;
    }

    @Transactional
    public EventApply submitEventApplication(EventApplyRequestDto requestDto) {

        if (userRepository.existsByEmail(requestDto.getEventEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 등록된 이메일입니다.");
        }

        if (eventApplyRepository.existsPendingApplicationByEventEmail(requestDto.getEventEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "해당 이메일로 이미 처리 대기 중인 신청이 존재합니다.");
        }

        EventApply eventApply = new EventApply();
        ApplyStatusCode pendingStatus = applyStatusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "대기 상태 코드를 찾을 수 없습니다."));
        eventApply.setStatusCode(pendingStatus);
        
        // 파일 URL 기본값 설정 (DB NOT NULL 제약조건 대응)
        eventApply.setFileUrl("");
        eventApply.setBannerUrl("");
        eventApply.setThumbnailUrl("");
        eventApply.setEventEmail(requestDto.getEventEmail());
        eventApply.setBusinessNumber(requestDto.getBusinessNumber());
        eventApply.setBusinessName(requestDto.getBusinessName());
        eventApply.setBusinessDate(requestDto.getBusinessDate());
        Boolean verifiedValue = requestDto.getVerified() != null ? requestDto.getVerified() : false;
        log.info("사업자 검증 상태 설정 - 요청값: {}, 설정값: {}", requestDto.getVerified(), verifiedValue);
        eventApply.setVerified(verifiedValue);
        eventApply.setManagerName(requestDto.getManagerName());
        eventApply.setEmail(requestDto.getEmail());
        eventApply.setContactNumber(requestDto.getContactNumber());
        eventApply.setTitleKr(requestDto.getTitleKr());
        eventApply.setTitleEng(requestDto.getTitleEng());

        // Location 처리 로직
        Location location = null;
        if (requestDto.getLocationId() != null) {
            // 기존 Location 사용
            location = locationRepository.findById(requestDto.getLocationId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "위치 정보를 찾을 수 없습니다."));
        } else if (requestDto.getAddress() != null && requestDto.getPlaceName() != null) {
            // 새로운 Location 생성 (카카오맵에서 받은 데이터)
            location = createLocationFromKakaoData(requestDto);
        } else if (requestDto.getLocationDetail() != null && !requestDto.getLocationDetail().trim().isEmpty()) {
            // locationDetail에 JSON 형태로 장소 정보가 있는 경우 파싱해서 처리
            location = createLocationFromLocationDetail(requestDto.getLocationDetail());
        }
        
        if (location != null) {
            eventApply.setLocation(location);
        }
        eventApply.setLocationDetail(requestDto.getLocationDetail());

        eventApply.setStartDate(requestDto.getStartDate());
        eventApply.setEndDate(requestDto.getEndDate());

        if (requestDto.getMainCategoryId() != null) {
            MainCategory mainCategory = mainCategoryRepository.findById(requestDto.getMainCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "메인 카테고리를 찾을 수 없습니다."));
            eventApply.setMainCategory(mainCategory);
        }

        if (requestDto.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryRepository.findById(requestDto.getSubCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "서브 카테고리를 찾을 수 없습니다."));
            eventApply.setSubCategory(subCategory);
        }

        if (requestDto.getTempFiles() != null && !requestDto.getTempFiles().isEmpty()) {
            processTempFilesAndSetUrls(eventApply, requestDto.getTempFiles());
        }

        EventApply savedEventApply = eventApplyRepository.save(eventApply);

        if (requestDto.getTempFiles() != null && !requestDto.getTempFiles().isEmpty()) {
            createFileRecordsForApply(savedEventApply, requestDto.getTempFiles());
        }

        return savedEventApply;
    }

    private void processTempFilesAndSetUrls(EventApply eventApply, List<EventApplyRequestDto.FileUploadDto> tempFiles) {
        for (EventApplyRequestDto.FileUploadDto fileDto : tempFiles) {
            try {
                log.info("Processing temporary file - S3 Key: {}, Usage: {}, Original Name: {}", 
                    fileDto.getS3Key(), fileDto.getUsage(), fileDto.getOriginalFileName());
                
                String directory = "event-apply/" + fileDto.getUsage();
                String newKey = awsS3Service.moveToPermanent(fileDto.getS3Key(), directory);
                String finalFileUrl = awsS3Service.getCdnUrl(newKey);

                switch (fileDto.getUsage().toLowerCase()) {
                    case "file":
                    case "application_file":
                        eventApply.setFileUrl(finalFileUrl);
                        log.info("Set fileUrl: {}", finalFileUrl);
                        break;
                    case "banner":
                        eventApply.setBannerUrl(finalFileUrl);
                        log.info("Set bannerUrl: {}", finalFileUrl);
                        break;
                    case "thumbnail":
                        eventApply.setThumbnailUrl(finalFileUrl);
                        log.info("Set thumbnailUrl: {}", finalFileUrl);
                        break;
                    default:
                        log.warn("Unknown file usage '{}' for temp key {}", fileDto.getUsage(), fileDto.getS3Key());
                }
            } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
                log.error("Temporary file not found in S3 - Key: {}, Original Name: {}. " +
                    "파일이 만료되었거나 이미 삭제되었을 수 있습니다.", fileDto.getS3Key(), fileDto.getOriginalFileName());
                throw new CustomException(HttpStatus.BAD_REQUEST, 
                    "임시 파일을 찾을 수 없습니다: " + fileDto.getOriginalFileName() + ". 파일을 다시 업로드해 주세요.");
            } catch (Exception e) {
                log.error("Error processing temporary file with key {}: {}", fileDto.getS3Key(), e.getMessage(), e);
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "파일 처리 중 오류가 발생했습니다: " + fileDto.getOriginalFileName());
            }
        }
    }

    private void createFileRecordsForApply(EventApply eventApply, List<EventApplyRequestDto.FileUploadDto> tempFiles) {
        for (EventApplyRequestDto.FileUploadDto fileDto : tempFiles) {
            try {
                String finalUrl;
                switch (fileDto.getUsage().toLowerCase()) {
                    case "file":
                    case "application_file":
                        finalUrl = eventApply.getFileUrl();
                        break;
                    case "banner":
                        finalUrl = eventApply.getBannerUrl();
                        break;
                    case "thumbnail":
                        finalUrl = eventApply.getThumbnailUrl();
                        break;
                    default:
                        continue;
                }

                if (finalUrl == null) {
                    log.warn("Final URL for usage '{}' is null, skipping file record creation.", fileDto.getUsage());
                    continue;
                }

                String s3Key = awsS3Service.getS3KeyFromPublicUrl(finalUrl);
                if (s3Key == null) {
                    log.error("Could not extract S3 key from URL: {}", finalUrl);
                    continue;
                }

                File file = File.builder()
                        .eventApply(eventApply)
                        .fileUrl(s3Key)
                        .referenced(true)
                        .fileType(fileDto.getFileType())
                        .directory("event-apply/" + fileDto.getUsage())
                        .originalFileName(fileDto.getOriginalFileName())
                        .storedFileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                        .fileSize(fileDto.getFileSize())
                        .thumbnail("thumbnail".equalsIgnoreCase(fileDto.getUsage()))
                        .build();

                fileRepository.save(file);

            } catch (Exception e) {
                log.error("Error creating file record for temp key {}: {}", fileDto.getS3Key(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void approveEventApplication(Long eventApplyId, String adminComment, Long adminId) {
        EventApply eventApply = eventApplyRepository.findById(eventApplyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사 신청을 찾을 수 없습니다."));

        if (!"PENDING".equals(eventApply.getStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "대기 중인 신청만 승인할 수 있습니다.");
        }

        String tempPassword = userService.generateRandomPassword(12);
        EventAdmin eventAdmin = createEventAdminAccount(eventApply, tempPassword);
        Event event = createEventFromApplication(eventApply, eventAdmin);
        EventDetail eventDetail = createEventDetailFromApplication(eventApply, event);
        moveFilesToEvent(eventApply, event.getEventId(), eventDetail);
        eventVersionService.createEventVersion(event, adminId);

        ApplyStatusCode approvedStatus = applyStatusCodeRepository.findByCode("APPROVED")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "승인 상태 코드를 찾을 수 없습니다."));
        eventApply.updateStatus(approvedStatus, adminComment);
        eventApplyRepository.save(eventApply);

        // Send approval email with account info
        eventEmailService.sendApprovalEmail(
            eventApply.getEmail(), 
            eventApply.getTitleKr(), 
            eventApply.getEventEmail(), // Username is event email
            tempPassword
        );

        // Send web notification to the new event admin
        try {
            NotificationRequestDto notificationDto = new NotificationRequestDto();
            notificationDto.setUserId(eventAdmin.getUserId());
            notificationDto.setTypeCode("EVENT_APPROVAL");
            notificationDto.setMethodCode("WEB");
            notificationDto.setTitle("행사 등록 승인");
            notificationDto.setMessage("'" + eventApply.getTitleKr() + "' 행사 등록 신청이 승인되었습니다. 관리자 계정으로 로그인하여 행사를 관리해보세요.");
            notificationDto.setUrl("/events/" + event.getEventId());
            
            notificationService.createNotification(notificationDto);
        } catch (Exception e) {
            log.error("웹 알림 전송 실패 - EventApply ID: {}, 오류: {}", eventApplyId, e.getMessage());
        }

        log.info("행사 신청이 승인되었습니다. eventApplyId: {}, eventId: {}", eventApplyId, event.getEventId());
    }

    @Transactional
    public void rejectEventApplication(Long eventApplyId, String adminComment) {
        EventApply eventApply = eventApplyRepository.findById(eventApplyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사 신청을 찾을 수 없습니다."));

        if (!"PENDING".equals(eventApply.getStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "대기 중인 신청만 반려할 수 있습니다.");
        }

        ApplyStatusCode rejectedStatus = applyStatusCodeRepository.findByCode("REJECTED")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "반려 상태 코드를 찾을 수 없습니다."));

        eventApply.updateStatus(rejectedStatus, adminComment);
        eventApplyRepository.save(eventApply);

        deleteEventApplyFiles(eventApply);

        // Send rejection email
        eventEmailService.sendRejectionEmail(
            eventApply.getEmail(), 
            eventApply.getTitleKr(), 
            adminComment
        );

        // Send web notification if the applicant has a user account
        try {
            userRepository.findByEmail(eventApply.getEmail()).ifPresent(user -> {
                NotificationRequestDto notificationDto = new NotificationRequestDto();
                notificationDto.setUserId(user.getUserId());
                notificationDto.setTypeCode("EVENT_REJECTION");
                notificationDto.setMethodCode("WEB");
                notificationDto.setTitle("행사 등록 반려");
                notificationDto.setMessage("'" + eventApply.getTitleKr() + "' 행사 등록 신청이 반려되었습니다. 사유: " + 
                    (adminComment != null && !adminComment.trim().isEmpty() ? adminComment : "관리자 검토 결과"));
                notificationDto.setUrl("/events/apply");
                
                try {
                    notificationService.createNotification(notificationDto);
                } catch (Exception e) {
                    log.error("웹 알림 전송 실패 - EventApply ID: {}, 오류: {}", eventApplyId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("사용자 조회 실패 - EventApply ID: {}, 오류: {}", eventApplyId, e.getMessage());
        }

        log.info("행사 신청이 반려되었습니다. eventApplyId: {}", eventApplyId);
    }

    private void deleteEventApplyFiles(EventApply eventApply) {
        try {
            List<String> urlsToDelete = new ArrayList<>();
            if (eventApply.getFileUrl() != null && !eventApply.getFileUrl().isEmpty()) {
                urlsToDelete.add(eventApply.getFileUrl());
            }
            if (eventApply.getBannerUrl() != null && !eventApply.getBannerUrl().isEmpty()) {
                urlsToDelete.add(eventApply.getBannerUrl());
            }
            if (eventApply.getThumbnailUrl() != null && !eventApply.getThumbnailUrl().isEmpty()) {
                urlsToDelete.add(eventApply.getThumbnailUrl());
            }

            for (String url : urlsToDelete) {
                try {
                    String s3Key = awsS3Service.getS3KeyFromPublicUrl(url);
                    if (s3Key != null) {
                        fileService.deleteFileByS3Key(s3Key);
                        log.info("파일 삭제 성공 - EventApply ID: {}, S3 Key: {}", eventApply.getEventApplyId(), s3Key);
                    }
                } catch (Exception e) {
                    log.error("개별 파일 삭제 실패 - EventApply ID: {}, URL: {}, 오류: {}",
                            eventApply.getEventApplyId(), url, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("EventApply 파일 삭제 중 오류 발생 - EventApply ID: {}, 오류: {}",
                    eventApply.getEventApplyId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<EventApply> getApplications(String status, Pageable pageable) {
        Page<EventApply> eventApplies;

        if (status == null || status.isEmpty()) {
            eventApplies = eventApplyRepository.findAll(pageable);
        } else {
            eventApplies = eventApplyRepository.findByStatusCode_CodeOrderByApplyAtDesc(status, pageable);
        }

        return eventApplies;
    }

    @Transactional(readOnly = true)
    public Optional<EventApply> findByEventEmail(String eventEmail) {
        return eventApplyRepository.findByEventEmail(eventEmail);
    }

    private EventAdmin createEventAdminAccount(EventApply eventApply, String tempPassword) {
        log.info("사용자 계정 생성 시작 - 이메일: {}, 이름: {}", eventApply.getEventEmail(), eventApply.getManagerName());
        
        // 이메일 중복 체크 (혹시 다른 곳에서 같은 이메일이 생성되었을 경우를 대비)
        if (userRepository.existsByEmail(eventApply.getEventEmail())) {
            log.error("이미 존재하는 이메일로 사용자 생성 시도: {}", eventApply.getEventEmail());
            throw new CustomException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다: " + eventApply.getEventEmail());
        }
        
        UserRoleCode eventAdminRole = userRoleCodeRepository.findByCode("EVENT_MANAGER")
                .orElseThrow(() -> {
                    log.error("EVENT_MANAGER 역할을 찾을 수 없음. 사용 가능한 역할을 확인하세요.");
                    return new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT_MANAGER 역할을 찾을 수 없습니다.");
                });
        
        log.info("EVENT_MANAGER 역할 찾기 성공 - 역할 ID: {}", eventAdminRole.getId());

        Users user = Users.builder()
                .email(eventApply.getEventEmail())
                .password(passwordEncoder.encode(tempPassword))
                .name(eventApply.getManagerName())
                .nickname(eventApply.getManagerName())
                .phone(eventApply.getContactNumber())
                .roleCode(eventAdminRole)
                .build(); // @PrePersist가 createdAt, updatedAt을 자동 설정

        log.info("사용자 엔티티 빌드 완료, 저장 시도 중");
        Users savedUser = userRepository.save(user);
        log.info("사용자 저장 성공 - 사용자 ID: {}", savedUser.getUserId());

        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUser(savedUser);
        eventAdmin.setBusinessNumber(eventApply.getBusinessNumber());
        eventAdmin.setContactNumber(eventApply.getContactNumber());
        eventAdmin.setContactEmail(eventApply.getEmail());
        eventAdmin.setActive(true);

        log.info("EventAdmin 엔티티 생성 완료, 저장 시도 중");
        EventAdmin savedEventAdmin = eventAdminRepository.save(eventAdmin);
        log.info("EventAdmin 저장 성공 - EventAdmin ID: {}", savedEventAdmin.getUserId());
        
        return savedEventAdmin;
    }

    private Event createEventFromApplication(EventApply eventApply, EventAdmin eventAdmin) {
        Event event = new Event();
        event.setManager(eventAdmin);

        EventStatusCode upcomingStatus = eventStatusCodeRepository.findById(1)
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "행사 상태 코드를 찾을 수 없습니다."));
        event.setStatusCode(upcomingStatus);

        event.setTitleKr(eventApply.getTitleKr());
        event.setTitleEng(eventApply.getTitleEng());
        event.setEventCode("TEMP");

        Event savedEvent = eventRepository.save(event);

        String eventCode = hashids.encode(savedEvent.getEventId());
        savedEvent.setEventCode("EVT-" + eventCode);

        return eventRepository.save(savedEvent);
    }

    private EventDetail createEventDetailFromApplication(EventApply eventApply, Event event) {
        EventDetail eventDetail = new EventDetail();
        eventDetail.setEvent(event);
        eventDetail.setLocation(eventApply.getLocation());
        eventDetail.setLocationDetail(eventApply.getLocationDetail());
        eventDetail.setStartDate(eventApply.getStartDate());
        eventDetail.setEndDate(eventApply.getEndDate());
        eventDetail.setMainCategory(eventApply.getMainCategory());
        eventDetail.setSubCategory(eventApply.getSubCategory());
        eventDetail.setBannerUrl(eventApply.getBannerUrl());
        eventDetail.setThumbnailUrl(eventApply.getThumbnailUrl());
        
        // RegionCode 설정 - Location의 주소에서 추출
        setRegionCodeFromLocation(eventDetail, eventApply);

        eventDetail.setContent("행사 내용을 입력해주세요.");
        eventDetail.setPolicy("행사 정책을 입력해주세요.");
        eventDetail.setHostName(eventApply.getManagerName());
        eventDetail.setContactInfo(eventApply.getEmail());
        eventDetail.setReentryAllowed(true);
        eventDetail.setCheckOutAllowed(false);

        EventDetail savedEventDetail = eventDetailRepository.save(eventDetail);

        event.setEventDetail(savedEventDetail);

        return savedEventDetail;
    }

    private void moveFilesToEvent(EventApply eventApply, Long eventId, EventDetail eventDetail) {
        try {
            // EventApply와 연결된 모든 File 엔티티 조회
            List<File> eventApplyFiles = fileRepository.findByEventApply(eventApply);
            
            for (File file : eventApplyFiles) {
                try {
                    String usage = determineFileUsage(file);
                    String newCdnUrl = fileService.moveFileToEvent(file.getFileUrl(), eventId, usage);
                    
                    // EventDetail URL 업데이트
                    switch (usage) {
                        case "banners":
                            eventDetail.setBannerUrl(newCdnUrl);
                            break;
                        case "thumbnails":
                            eventDetail.setThumbnailUrl(newCdnUrl);
                            break;
                        // documents는 EventDetail에 URL 필드가 없으므로 File 엔티티에만 저장
                    }
                    
                    log.info("파일 이동 성공 - EventApply: {}, File: {}, Usage: {}", 
                        eventApply.getEventApplyId(), file.getId(), usage);
                        
                } catch (Exception e) {
                    log.error("개별 파일 이동 실패 - EventApply: {}, File: {}, 오류: {}", 
                        eventApply.getEventApplyId(), file.getId(), e.getMessage());
                }
            }

            eventDetailRepository.save(eventDetail);
            log.info("파일 이동 완료 - EventId: {}, EventApply ID: {}", eventId, eventApply.getEventApplyId());

        } catch (Exception e) {
            log.error("파일 이동 중 오류 발생 - EventApply ID: {}, 오류: {}",
                    eventApply.getEventApplyId(), e.getMessage());
        }
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
        
        // 기본값은 documents
        return "documents";
    }

    private void sendAccountCreationEmail(EventApply eventApply, String tempPassword, String adminComment) {
        try {
            temporaryPasswordEmailService.send(
                eventApply.getEmail(), 
                eventApply.getTitleKr(), 
                eventApply.getEventEmail(), 
                tempPassword
            );
            log.info("계정 생성 이메일 전송 완료 - 받는 사람: {}", eventApply.getEmail());
            if (adminComment != null && !adminComment.trim().isEmpty()) {
                log.info("관리자 메모: {}", adminComment);
            }
        } catch (Exception e) {
            log.error("계정 생성 이메일 전송 실패 - 받는 사람: {}, 오류: {}", 
                    eventApply.getEmail(), e.getMessage());
        }
    }

    private void setRegionCodeFromLocation(EventDetail eventDetail, EventApply eventApply) {
        if (eventApply.getLocation() != null && eventApply.getLocation().getAddress() != null) {
            try {
                String address = eventApply.getLocation().getAddress();
                String regionName = address.substring(0, 2); // 주소에서 앞 2글자 추출 (예: "서울", "부산")
                
                RegionCode regionCode = regionCodeRepository.findByName(regionName)
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, 
                            "해당 지역코드를 찾지 못했습니다: " + regionName));
                
                eventDetail.setRegionCode(regionCode);
                log.info("RegionCode 설정 완료 - EventApply ID: {}, Region: {}", 
                    eventApply.getEventApplyId(), regionName);
            } catch (Exception e) {
                log.error("RegionCode 설정 실패 - EventApply ID: {}, 오류: {}", 
                    eventApply.getEventApplyId(), e.getMessage());
                // RegionCode 설정에 실패하면 기본값으로 서울 설정
                RegionCode defaultRegionCode = regionCodeRepository.findByName("서울")
                        .orElse(null);
                if (defaultRegionCode != null) {
                    eventDetail.setRegionCode(defaultRegionCode);
                    log.info("기본 RegionCode(서울) 설정 완료 - EventApply ID: {}", eventApply.getEventApplyId());
                }
            }
        }
    }

    /**
     * 카카오맵 데이터로부터 새로운 Location 생성
     */
    private Location createLocationFromKakaoData(EventApplyRequestDto requestDto) {
        log.info("카카오맵 데이터로 Location 생성 시작 - placeName: {}, address: {}", 
                requestDto.getPlaceName(), requestDto.getAddress());
        
        // 기존에 같은 placeName으로 등록된 Location이 있는지 확인
        Location existingLocation = locationRepository.findByPlaceName(requestDto.getPlaceName());
        if (existingLocation != null) {
            log.info("기존 Location 사용 - placeName: {}, locationId: {}", 
                    requestDto.getPlaceName(), existingLocation.getLocationId());
            return existingLocation;
        }
        
        // 새로운 Location 생성
        Location location = new Location();
        location.setPlaceName(requestDto.getPlaceName());
        location.setAddress(requestDto.getAddress());
        location.setLatitude(requestDto.getLatitude());
        location.setLongitude(requestDto.getLongitude());
        location.setPlaceUrl(requestDto.getPlaceUrl());
        
        Location savedLocation = locationRepository.save(location);
        log.info("새로운 Location 생성 완료 - locationId: {}, placeName: {}", 
                savedLocation.getLocationId(), savedLocation.getPlaceName());
        
        return savedLocation;
    }

    /**
     * locationDetail JSON에서 장소 정보를 파싱하여 Location 생성
     */
    private Location createLocationFromLocationDetail(String locationDetail) {
        try {
            log.info("locationDetail JSON 파싱 시작: {}", locationDetail);
            
            // JSON 파싱 (간단한 방식으로 처리)
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(locationDetail);
            
            String placeName = jsonNode.has("placeName") ? jsonNode.get("placeName").asText() : null;
            String address = jsonNode.has("address") ? jsonNode.get("address").asText() : null;
            
            if (placeName == null || address == null) {
                log.warn("필수 장소 정보가 없음 - placeName: {}, address: {}", placeName, address);
                return null;
            }
            
            // 기존에 같은 placeName으로 등록된 Location이 있는지 확인
            Location existingLocation = locationRepository.findByPlaceName(placeName);
            if (existingLocation != null) {
                log.info("기존 Location 사용 - placeName: {}, locationId: {}", 
                        placeName, existingLocation.getLocationId());
                return existingLocation;
            }
            
            // 새로운 Location 생성
            Location location = new Location();
            location.setPlaceName(placeName);
            location.setAddress(address);
            
            if (jsonNode.has("latitude") && !jsonNode.get("latitude").isNull()) {
                location.setLatitude(new java.math.BigDecimal(jsonNode.get("latitude").asDouble()));
            }
            if (jsonNode.has("longitude") && !jsonNode.get("longitude").isNull()) {
                location.setLongitude(new java.math.BigDecimal(jsonNode.get("longitude").asDouble()));
            }
            if (jsonNode.has("placeUrl") && !jsonNode.get("placeUrl").isNull()) {
                location.setPlaceUrl(jsonNode.get("placeUrl").asText());
            }
            
            Location savedLocation = locationRepository.save(location);
            log.info("JSON에서 새로운 Location 생성 완료 - locationId: {}, placeName: {}", 
                    savedLocation.getLocationId(), savedLocation.getPlaceName());
            
            return savedLocation;
            
        } catch (Exception e) {
            log.error("locationDetail JSON 파싱 실패: {}, 오류: {}", locationDetail, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public EventApplyResponseDto getEventApplicationDetail(Long applicationId) {
        EventApply entity = eventApplyRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다.", null));

        return EventApplyResponseDto.builder()
                .eventApplyId(entity.getEventApplyId())
                .statusCode(entity.getStatusCode().getCode())
                .statusName(entity.getStatusCode().getName())
                .eventEmail(entity.getEventEmail())
                .businessNumber(entity.getBusinessNumber())
                .businessName(entity.getBusinessName())
                .businessDate(entity.getBusinessDate())
                .verified(entity.getVerified())
                .managerName(entity.getManagerName())
                .contactNumber(entity.getContactNumber())
                .email(entity.getEmail())
                .titleKr(entity.getTitleKr())
                .titleEng(entity.getTitleEng())
                .fileUrl(entity.getFileUrl())
                .applyAt(entity.getApplyAt())
                .adminComment(entity.getAdminComment())
                .statusUpdatedAt(entity.getStatusUpdatedAt())
                .locationId(entity.getLocation().getLocationId())
                .address(entity.getLocation().getAddress())
                .locationName(entity.getLocation().getPlaceName())
                .locationDetail(entity.getLocationDetail())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .mainCategoryName(entity.getMainCategory().getGroupName())
                .subCategoryName(entity.getSubCategory().getCategoryName())
                .bannerUrl(entity.getBannerUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .build();
    }
}