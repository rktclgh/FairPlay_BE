package com.fairing.fairplay.event.service;

import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.EventEmailService;
import com.fairing.fairplay.core.email.service.TemporaryPasswordEmailService;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.dto.EventApplyRequestDto;
import com.fairing.fairplay.event.dto.EventApplyResponseDto;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.TempFileUploadDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import com.fairing.fairplay.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
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
    private final SuperAdminService superAdminService;
    private final EventStatusCodeRepository statusCodeRepository;

    private static final String NOT_FOUND_STATUS = "해당 상태 코드 없음";

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
        eventApply.setVerified(verifiedValue);
        eventApply.setManagerName(requestDto.getManagerName());
        eventApply.setEmail(requestDto.getEmail());
        eventApply.setContactNumber(requestDto.getContactNumber());
        eventApply.setTitleKr(requestDto.getTitleKr());
        eventApply.setTitleEng(requestDto.getTitleEng());

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

        EventApply savedEventApply = eventApplyRepository.save(eventApply);

        if (requestDto.getTempFiles() != null && !requestDto.getTempFiles().isEmpty()) {
            processAndLinkFiles(savedEventApply, requestDto.getTempFiles());
        }

        return savedEventApply;
    }

    private void processAndLinkFiles(EventApply eventApply, List<TempFileUploadDto> tempFiles) {
        for (TempFileUploadDto fileDto : tempFiles) {
            try {
                String directory = "event-apply/" + fileDto.getUsage();

                File savedFile = fileService.uploadFile(S3UploadRequestDto.builder()
                        .s3Key(fileDto.getS3Key())
                        .originalFileName(fileDto.getOriginalFileName())
                        .fileType(fileDto.getFileType())
                        .fileSize(fileDto.getFileSize())
                        .directoryPrefix(directory)
                        .usage(fileDto.getUsage())
                        .build());

                fileService.createFileLink(savedFile, "EVENT_APPLY", eventApply.getEventApplyId());

                String cdnUrl = awsS3Service.getCdnUrl(savedFile.getFileUrl());

                switch (fileDto.getUsage().toLowerCase()) {
                    case "file":
                    case "application_file":
                        eventApply.setFileUrl(cdnUrl);
                        break;
                    case "banner":
                        eventApply.setBannerUrl(cdnUrl);
                        break;
                    case "thumbnail":
                        eventApply.setThumbnailUrl(cdnUrl);
                        break;
                    default:
                        log.warn("Unknown file usage '{}' for temp key {}", fileDto.getUsage(), fileDto.getS3Key());
                }
            } catch (Exception e) {
                log.error("Error processing temporary file with key {}: {}", fileDto.getS3Key(), e.getMessage(), e);
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다: " + fileDto.getOriginalFileName());
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

        EventStatusCode upcoming = statusCodeRepository.findByCode("UPCOMING")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
        EventStatusCode ongoing = statusCodeRepository.findByCode("ONGOING")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
        EventStatusCode ended = statusCodeRepository.findByCode("ENDED")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));


        LocalDate today = LocalDate.now();
        EventStatusCode newStatus = determineStatus(today, event.getEventDetail(), upcoming, ongoing, ended);
        event.setStatusCode(newStatus);
        moveFilesToEvent(eventApply, event.getEventId(), eventDetail);

        eventVersionService.createEventVersion(event, adminId);

        ApplyStatusCode approvedStatus = applyStatusCodeRepository.findByCode("APPROVED")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "승인 상태 코드를 찾을 수 없습니다."));
        eventApply.updateStatus(approvedStatus, adminComment);
        eventApplyRepository.save(eventApply);

        eventEmailService.sendApprovalEmail(
                eventApply.getEmail(),
                eventApply.getTitleKr(),
                eventApply.getEventEmail(),
                tempPassword);

        try {
            NotificationRequestDto notificationDto = new NotificationRequestDto();
            notificationDto.setUserId(eventAdmin.getUserId());
            notificationDto.setTypeCode("EVENT_APPROVAL");
            notificationDto.setMethodCode("WEB");
            notificationDto.setTitle("행사 등록 승인");
            notificationDto
                    .setMessage("'" + eventApply.getTitleKr() + "' 행사 등록 신청이 승인되었습니다. 관리자 계정으로 로그인하여 행사를 관리해보세요.");
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

        eventEmailService.sendRejectionEmail(
                eventApply.getEmail(),
                eventApply.getTitleKr(),
                adminComment);

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
            List<File> filesToDelete = fileRepository.findByTargetTypeAndTargetId("EVENT_APPLY", eventApply.getEventApplyId());
            for (File file : filesToDelete) {
                fileService.deleteFile(file.getId());
            }
            log.info("EventApply 파일 삭제 완료 - EventApply ID: {}", eventApply.getEventApplyId());
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
                .build();

        log.info("사용자 엔티티 빌드 완료, 저장 시도 중");
        Users savedUser = userRepository.save(user);
        log.info("사용자 저장 성공 - 사용자 ID: {}", savedUser.getUserId());

        superAdminService.setEventAdmin(savedUser.getUserId());

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

    private EventStatusCode determineStatus(LocalDate today, EventDetail eventDetail,
                                            EventStatusCode upcoming,
                                            EventStatusCode ongoing,
                                            EventStatusCode ended) {

        if (eventDetail == null) {
            // EventDetail이 없는 경우
            throw new CustomException(HttpStatus.NOT_FOUND, "행사 상세 정보가 존재하지 않습니다. 먼저 상세 정보를 등록하세요.");
        }

        if (today.isBefore(eventDetail.getStartDate())) {
            return upcoming;
        } else if (!today.isAfter(eventDetail.getEndDate())) {
            return ongoing;
        } else {
            return ended;
        }
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

        eventDetail.setContent("");
        eventDetail.setPolicy("");
        eventDetail.setHostName(eventApply.getManagerName());
        eventDetail.setContactInfo("");
        eventDetail.setReentryAllowed(true);
        eventDetail.setCheckOutAllowed(false);

        EventDetail savedEventDetail = eventDetailRepository.save(eventDetail);

        event.setEventDetail(savedEventDetail);

        return savedEventDetail;
    }

    private void moveFilesToEvent(EventApply eventApply, Long eventId, EventDetail eventDetail) {
        try {
            List<File> eventApplyFiles = fileRepository.findByTargetTypeAndTargetId("EVENT_APPLY", eventApply.getEventApplyId());

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

        return "documents";
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
            
            // JSON 파싱
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