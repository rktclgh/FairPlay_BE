package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.core.util.JsonUtil;
import com.fairing.fairplay.event.dto.EventDetailModificationDto;
import com.fairing.fairplay.event.dto.EventDetailRequestDto;
import com.fairing.fairplay.event.dto.EventSnapshotDto;
import com.fairing.fairplay.event.dto.ExternalLinkRequestDto;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventDetailModificationRequestService {

    private final EventDetailModificationRequestRepository modificationRequestRepository;
    private final UpdateStatusCodeRepository updateStatusCodeRepository;
    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final LocationRepository locationRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final EventAdminRepository eventAdminRepository;
    private final UserRepository userRepository;
    private final ExternalLinkRepository externalLinkRepository;
    private final EventVersionService eventVersionService;
    private final AwsS3Service awsS3Service;
    private final FileService fileService;

    // 수정 요청
    @Transactional
    public EventDetailModificationRequest createModificationRequest(Long eventId, EventDetailModificationDto modificationDto, Long requestedBy) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        if (modificationRequestRepository.existsPendingRequestByEventId(eventId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 처리 대기 중인 수정 요청이 존재합니다.");
        }

        EventDetail currentEventDetail = event.getEventDetail();
        if (currentEventDetail == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "행사 상세 정보를 찾을 수 없습니다.");
        }

        List<String> newFileKeys = new ArrayList<>();
        Map<String, String> urlMappings = new HashMap<>(); // 임시 URL -> 영구 URL 매핑
        
        if (modificationDto.getTempFiles() != null && !modificationDto.getTempFiles().isEmpty()) {
            for (EventDetailRequestDto.FileUploadDto fileDto : modificationDto.getTempFiles()) {
                try {
                    String directory = "events/" + eventId + "/" + fileDto.getUsage();
                    String newKey = awsS3Service.moveToPermanent(fileDto.getS3Key(), directory);
                    String cdnUrl = awsS3Service.getCdnUrl(newKey);
                    newFileKeys.add(newKey);

                    // 임시 URL과 영구 URL 매핑 저장
                    String tempCdnUrl = awsS3Service.getCdnUrl(fileDto.getS3Key());
                    urlMappings.put(tempCdnUrl, cdnUrl);

                    switch (fileDto.getUsage().toLowerCase()) {
                        case "banner":
                        case "banner_horizontal":
                            modificationDto.setBannerUrl(cdnUrl);
                            break;
                        case "thumbnail":
                        case "banner_vertical":
                            modificationDto.setThumbnailUrl(cdnUrl);
                            break;
                        case "content_image":
                            // content_image는 content 내에서 URL 치환으로 처리
                            break;
                        default:
                            log.info("처리되지 않은 파일 용도: {}", fileDto.getUsage());
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing temporary file for modification request: {}", e.getMessage());
                    throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다: " + fileDto.getOriginalFileName());
                }
            }
        }

        // content, policy, bio 내의 임시 URL을 영구 URL로 치환
        if (!urlMappings.isEmpty()) {
            if (modificationDto.getContent() != null) {
                String updatedContent = replaceUrlsInContent(modificationDto.getContent(), urlMappings);
                modificationDto.setContent(updatedContent);
            }
            if (modificationDto.getPolicy() != null) {
                String updatedPolicy = replaceUrlsInContent(modificationDto.getPolicy(), urlMappings);
                modificationDto.setPolicy(updatedPolicy);
            }
            if (modificationDto.getBio() != null) {
                String updatedBio = replaceUrlsInContent(modificationDto.getBio(), urlMappings);
                modificationDto.setBio(updatedBio);
            }
        }
        
        // 삭제할 파일 처리
        if (modificationDto.getDeletedFileIds() != null && !modificationDto.getDeletedFileIds().isEmpty()) {
            for (Long fileId : modificationDto.getDeletedFileIds()) {
                try {
                    log.info("파일 삭제 시작: fileId={}", fileId);
                    fileService.deleteFile(fileId);
                    log.info("파일 삭제 완료: fileId={}", fileId);
                } catch (Exception e) {
                    log.error("파일 삭제 실패: fileId={}, error={}", fileId, e.getMessage());
                    // 파일 삭제 실패 시에도 수정 요청은 계속 진행
                }
            }
        }

        UpdateStatusCode pendingStatus = updateStatusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "대기 상태 코드를 찾을 수 없습니다."));

        EventDetailModificationDto originalDto = convertEventDetailToDto(currentEventDetail);

        EventDetailModificationRequest request = new EventDetailModificationRequest();
        request.setEvent(event);
        request.setRequestedBy(requestedBy);
        request.setOriginalDataFromDto(originalDto);
        request.setModifiedDataFromDto(modificationDto);
        request.setStatus(pendingStatus);
        if (!newFileKeys.isEmpty()) {
            request.setNewFileKeysJson(JsonUtil.toJson(newFileKeys));
        }

        return modificationRequestRepository.save(request);
    }

    @Transactional
    public void approveModificationRequest(Long requestId, Long approvedBy, String adminComment) {
        EventDetailModificationRequest request = modificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "수정 요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(request.getStatus().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "대기 중인 요청만 승인할 수 있습니다.");
        }

        UpdateStatusCode approvedStatus = updateStatusCodeRepository.findByCode("APPROVED")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "승인 상태 코드를 찾을 수 없습니다."));

        request.setStatus(approvedStatus);
        request.approve(approvedBy, adminComment);

        // 승인 시점에서 임시 URL을 영구 URL로 변환
        EventDetailModificationDto modifiedData = request.getModifiedDataAsDto();
        Long eventId = request.getEvent().getEventId();
        
        if (modifiedData.getThumbnailUrl() != null && modifiedData.getThumbnailUrl().contains("/tmp")) {
            String s3Key = awsS3Service.getS3KeyFromPublicUrl(modifiedData.getThumbnailUrl());
            if (s3Key != null && s3Key.contains("/tmp")) {
                try {
                    String directory = "events/" + eventId + "/banner_vertical";
                    String newKey = awsS3Service.moveToPermanent(s3Key, directory);
                    String permanentUrl = awsS3Service.getCdnUrl(newKey);
                    modifiedData.setThumbnailUrl(permanentUrl);
                    log.info("승인 시점에서 임시 썸네일을 영구 저장소로 이동: {} -> {}", s3Key, newKey);
                    log.info("업데이트된 썸네일 URL: {}", permanentUrl);
                } catch (Exception e) {
                    log.error("승인 시점에서 임시 썸네일 이동 실패: {}", e.getMessage());
                }
            }
        }
        
        if (modifiedData.getBannerUrl() != null && modifiedData.getBannerUrl().contains("/tmp")) {
            String s3Key = awsS3Service.getS3KeyFromPublicUrl(modifiedData.getBannerUrl());
            if (s3Key != null && s3Key.contains("/tmp")) {
                try {
                    String directory = "events/" + eventId + "/banner_horizontal";
                    String newKey = awsS3Service.moveToPermanent(s3Key, directory);
                    String permanentUrl = awsS3Service.getCdnUrl(newKey);
                    modifiedData.setBannerUrl(permanentUrl);
                    log.info("승인 시점에서 임시 배너를 영구 저장소로 이동: {} -> {}", s3Key, newKey);
                    log.info("업데이트된 배너 URL: {}", permanentUrl);
                } catch (Exception e) {
                    log.error("승인 시점에서 임시 배너 이동 실패: {}", e.getMessage());
                }
            }
        }
        
        // 업데이트된 modifiedData를 다시 저장
        request.setModifiedDataFromDto(modifiedData);

        applyModificationToEventDetail(request);

        eventVersionService.createNewVersion(request.getEvent().getEventId(), approvedBy);

        modificationRequestRepository.save(request);
        log.info("행사 상세 수정 요청이 승인되었습니다. requestId: {}, eventId: {}", requestId, request.getEvent().getEventId());
    }

    @Transactional
    public void rejectModificationRequest(Long requestId, Long rejectedBy, String adminComment) {
        EventDetailModificationRequest request = modificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "수정 요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(request.getStatus().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "대기 중인 요청만 반려할 수 있습니다.");
        }

        // Delete orphaned files from S3
        if (request.getNewFileKeysJson() != null && !request.getNewFileKeysJson().isEmpty()) {
            List<String> fileKeysToDelete = JsonUtil.fromJson(request.getNewFileKeysJson(), List.class);
            for (String key : fileKeysToDelete) {
                try {
                    awsS3Service.deleteFile(key);
                    log.info("Deleted orphaned S3 file from rejected request: {}", key);
                } catch (Exception e) {
                    log.error("Failed to delete orphaned S3 file {}: {}", key, e.getMessage());
                }
            }
        }

        UpdateStatusCode rejectedStatus = updateStatusCodeRepository.findByCode("REJECTED")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "반려 상태 코드를 찾을 수 없습니다."));

        request.setStatus(rejectedStatus);
        request.reject(rejectedBy, adminComment);

        modificationRequestRepository.save(request);
        log.info("행사 상세 수정 요청이 반려되었습니다. requestId: {}, eventId: {}", requestId, request.getEvent().getEventId());
    }

    @Transactional(readOnly = true)
    public Page<EventDetailModificationRequest> getPendingRequests(Pageable pageable) {
        return modificationRequestRepository.findByStatus_CodeOrderByCreatedAtDesc("PENDING", pageable);
    }

    @Transactional(readOnly = true)
    public Optional<EventDetailModificationRequest> getPendingRequestByEventId(Long eventId) {
        return modificationRequestRepository.findPendingRequestByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Page<EventDetailModificationRequest> getModificationRequests(String status, Long eventId, Long requestedBy, Pageable pageable) {
        return modificationRequestRepository.findWithFilters(status, eventId, requestedBy, pageable);
    }

    @Transactional(readOnly = true)
    public EventDetailModificationRequest getModificationRequestById(Long requestId) {
        return modificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 수정 요청을 찾을 수 없습니다."));
    }

    private void applyModificationToEventDetail(EventDetailModificationRequest request) {
        Event event = request.getEvent();
        EventDetail eventDetail = event.getEventDetail();
        EventAdmin eventAdmin = event.getManager();
        Users eventAdminUser = event.getManager().getUser();
        EventDetailModificationDto modifiedData = request.getModifiedDataAsDto();

        // Event 엔티티의 제목 필드 업데이트
        if (modifiedData.getTitleKr() != null) event.setTitleKr(modifiedData.getTitleKr());
        if (modifiedData.getTitleEng() != null) event.setTitleEng(modifiedData.getTitleEng());

        // Location 정보 처리
        if (modifiedData.getLocationId() != null) {
            Location location = locationRepository.findById(modifiedData.getLocationId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "위치 정보를 찾을 수 없습니다."));
            eventDetail.setLocation(location);
        } else if (modifiedData.getPlaceName() != null || modifiedData.getAddress() != null || 
                   modifiedData.getLatitude() != null || modifiedData.getLongitude() != null || 
                   modifiedData.getPlaceUrl() != null) {
            // Location 정보가 변경된 경우 새로운 Location 생성 또는 기존 Location 업데이트
            Location location = eventDetail.getLocation();
            if (location == null) {
                location = new Location();
            }
            
            if (modifiedData.getPlaceName() != null) location.setPlaceName(modifiedData.getPlaceName());
            if (modifiedData.getAddress() != null) {
                location.setAddress(modifiedData.getAddress());
                String regionName = modifiedData.getAddress().substring(0, 2);
                RegionCode regionCode = regionCodeRepository.findByName(regionName)
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                                "해당 지역코드를 찾지 못했습니다: " + regionName));

                eventDetail.setRegionCode(regionCode);
            }
            if (modifiedData.getLatitude() != null) location.setLatitude(modifiedData.getLatitude());
            if (modifiedData.getLongitude() != null) location.setLongitude(modifiedData.getLongitude());
            if (modifiedData.getPlaceUrl() != null) location.setPlaceUrl(modifiedData.getPlaceUrl());
            
            location = locationRepository.save(location);
            eventDetail.setLocation(location);
        }

        if (modifiedData.getMainCategoryId() != null) {
            MainCategory mainCategory = mainCategoryRepository.findById(modifiedData.getMainCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "메인 카테고리를 찾을 수 없습니다."));
            eventDetail.setMainCategory(mainCategory);
        }

        if (modifiedData.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryRepository.findById(modifiedData.getSubCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "서브 카테고리를 찾을 수 없습니다."));
            eventDetail.setSubCategory(subCategory);
        }

        // null이 아닌(변경 요청된) 필드만 업데이트
        if (modifiedData.getLocationDetail() != null) eventDetail.setLocationDetail(modifiedData.getLocationDetail());
        if (modifiedData.getHostName() != null) eventDetail.setHostName(modifiedData.getHostName());
        if (modifiedData.getHostCompany() != null) eventDetail.setHostCompany(modifiedData.getHostCompany());
        if (modifiedData.getContactInfo() != null) eventDetail.setContactInfo(modifiedData.getContactInfo());
        if (modifiedData.getBio() != null) eventDetail.setBio(modifiedData.getBio());
        if (modifiedData.getContent() != null) eventDetail.setContent(modifiedData.getContent());
        if (modifiedData.getPolicy() != null) eventDetail.setPolicy(modifiedData.getPolicy());
        // officialUrl 처리 (공식 웹사이트 URL)
        if (modifiedData.getOfficialUrl() != null) {
            eventDetail.setOfficialUrl(modifiedData.getOfficialUrl());
        }
        
        // 외부 링크 처리 (ExternalLink 엔티티로 저장)
        if (modifiedData.getExternalLinks() != null) {
            // 승인 처리 전 기존 외부 링크 상태 로깅
            List<ExternalLink> existingLinks = externalLinkRepository.findByEvent(event);
            log.info("승인 처리 전 기존 외부 링크 개수: {}", existingLinks.size());
            for (ExternalLink link : existingLinks) {
                log.info("기존 링크: URL={}, DisplayText={}", link.getUrl(), link.getDisplayText());
            }
            
            // 수정 요청의 외부 링크 상태 로깅
            log.info("승인 처리할 외부 링크 개수: {}", modifiedData.getExternalLinks().size());
            for (ExternalLinkRequestDto linkDto : modifiedData.getExternalLinks()) {
                log.info("승인 처리할 링크: URL={}, DisplayText={}", linkDto.getUrl(), linkDto.getDisplayText());
            }
            
            // 기존 외부 링크 삭제
            log.info("기존 외부 링크 삭제 시작");
            externalLinkRepository.deleteByEvent(event);
            log.info("기존 외부 링크 삭제 완료");
            
            // 새로운 외부 링크 저장
            log.info("새로운 외부 링크 저장 시작");
            for (ExternalLinkRequestDto linkDto : modifiedData.getExternalLinks()) {
                if (linkDto.getUrl() != null && !linkDto.getUrl().isEmpty() &&
                    linkDto.getDisplayText() != null && !linkDto.getDisplayText().isEmpty()) {
                    ExternalLink externalLink = new ExternalLink();
                    externalLink.setEvent(event);
                    externalLink.setUrl(linkDto.getUrl());
                    externalLink.setDisplayText(linkDto.getDisplayText());
                    externalLinkRepository.save(externalLink);
                    log.info("외부 링크 저장 완료: URL={}, DisplayText={}", linkDto.getUrl(), linkDto.getDisplayText());
                } else {
                    log.warn("외부 링크 저장 건너뜀 - URL 또는 DisplayText가 비어있음: URL={}, DisplayText={}", 
                             linkDto.getUrl(), linkDto.getDisplayText());
                }
            }
            
            // 승인 처리 후 외부 링크 상태 로깅
            List<ExternalLink> finalLinks = externalLinkRepository.findByEvent(event);
            log.info("승인 처리 후 최종 외부 링크 개수: {}", finalLinks.size());
            for (ExternalLink link : finalLinks) {
                log.info("최종 링크: URL={}, DisplayText={}", link.getUrl(), link.getDisplayText());
            }
        }
        if (modifiedData.getEventTime() != null) eventDetail.setEventTime(modifiedData.getEventTime());
        if (modifiedData.getThumbnailUrl() != null) {
            log.info("썸네일 URL 업데이트: 기존={}, 새로운={}", eventDetail.getThumbnailUrl(), modifiedData.getThumbnailUrl());
            eventDetail.setThumbnailUrl(modifiedData.getThumbnailUrl());
        }
        if (modifiedData.getBannerUrl() != null) {
            log.info("배너 URL 업데이트: 기존={}, 새로운={}", eventDetail.getBannerUrl(), modifiedData.getBannerUrl());
            eventDetail.setBannerUrl(modifiedData.getBannerUrl());
        }
        if (modifiedData.getStartDate() != null) eventDetail.setStartDate(modifiedData.getStartDate());
        if (modifiedData.getEndDate() != null) eventDetail.setEndDate(modifiedData.getEndDate());
        if (modifiedData.getReentryAllowed() != null) eventDetail.setReentryAllowed(modifiedData.getReentryAllowed());
        if (modifiedData.getCheckInAllowed() != null) eventDetail.setCheckInAllowed(modifiedData.getCheckInAllowed());
        if (modifiedData.getCheckOutAllowed() != null) eventDetail.setCheckOutAllowed(modifiedData.getCheckOutAllowed());
        if (modifiedData.getAge() != null) eventDetail.setAge(modifiedData.getAge());

        // EventAdmin 정보 업데이트
        if (modifiedData.getBusinessNumber() != null) eventAdmin.setBusinessNumber(modifiedData.getBusinessNumber());
        if (modifiedData.getVerified() != null) eventAdmin.setVerified(modifiedData.getVerified());
        if (modifiedData.getManagerName() != null) eventAdminUser.setName(modifiedData.getManagerName());
        if (modifiedData.getManagerPhone() != null) eventAdmin.setContactNumber(modifiedData.getManagerPhone());
        if (modifiedData.getManagerEmail() != null) eventAdmin.setContactEmail(modifiedData.getManagerEmail());

        eventRepository.save(event);
        eventDetailRepository.save(eventDetail);
        eventAdminRepository.save(eventAdmin);
        if (eventAdminUser != null) {
            userRepository.save(eventAdminUser);
            log.info("Users 엔티티 저장 완료");
        }
    }

    private EventDetailModificationDto convertEventDetailToDto(EventDetail eventDetail) {
        EventDetailModificationDto dto = new EventDetailModificationDto();
        
        // Event 엔티티의 정보도 포함
        Event event = eventDetail.getEvent();
        if (event != null) {
            dto.setTitleKr(event.getTitleKr());
            dto.setTitleEng(event.getTitleEng());
            
            // EventAdmin 정보 포함 - 강제 로드
            EventAdmin eventAdmin = event.getManager();
            log.info("EventAdmin 정보: {}", eventAdmin != null ? "존재함" : "null");
            if (eventAdmin != null) {
                // Lazy loading 강제 초기화
                try {
                    String businessNumber = eventAdmin.getBusinessNumber();
                    String contactNumber = eventAdmin.getContactNumber();
                    String contactEmail = eventAdmin.getContactEmail();
                    Boolean verified = eventAdmin.getVerified();

                    dto.setBusinessNumber(businessNumber);
                    dto.setVerified(verified);
                    dto.setManagerPhone(contactNumber);
                    dto.setManagerEmail(contactEmail);
                    
                    Users user = eventAdmin.getUser();
                    if (user != null) {
                        String userName = user.getName();
                        dto.setManagerName(userName);
                    } else {
                        log.info("EventAdmin.User is null");
                    }
                } catch (Exception e) {
                    log.error("EventAdmin 정보 로드 실패: {}", e.getMessage());
                }
            } else {
                log.info("Event.getManager() is null - manager_id가 설정되지 않았거나 연결된 EventAdmin이 없음");
            }
        } else {
            log.info("Event is null");
        }
        
        // Location 정보 - placeName과 address 매핑
        if (eventDetail.getLocation() != null) {
            dto.setLocationId(eventDetail.getLocation().getLocationId());
            dto.setPlaceName(eventDetail.getLocation().getPlaceName());
            dto.setAddress(eventDetail.getLocation().getAddress());
            dto.setLatitude(eventDetail.getLocation().getLatitude());
            dto.setLongitude(eventDetail.getLocation().getLongitude());
            dto.setPlaceUrl(eventDetail.getLocation().getPlaceUrl());
        }
        
        dto.setLocationDetail(eventDetail.getLocationDetail());
        dto.setHostName(eventDetail.getHostName());
        dto.setContactInfo(eventDetail.getContactInfo());
        dto.setBio(eventDetail.getBio());
        dto.setContent(eventDetail.getContent());
        dto.setPolicy(eventDetail.getPolicy());
        dto.setOfficialUrl(eventDetail.getOfficialUrl());
        
        // ExternalLink 엔티티에서 외부 링크 조회
        List<ExternalLink> externalLinks = externalLinkRepository.findByEvent(eventDetail.getEvent());
        if (!externalLinks.isEmpty()) {
            List<ExternalLinkRequestDto> externalLinkDtos = externalLinks.stream()
                .map(link -> {
                    ExternalLinkRequestDto linkDto = new ExternalLinkRequestDto();
                    linkDto.setUrl(link.getUrl());
                    linkDto.setDisplayText(link.getDisplayText());
                    return linkDto;
                })
                .toList();
            dto.setExternalLinks(externalLinkDtos);
        }
        dto.setEventTime(eventDetail.getEventTime());
        dto.setThumbnailUrl(eventDetail.getThumbnailUrl());
        dto.setBannerUrl(eventDetail.getBannerUrl());
        dto.setStartDate(eventDetail.getStartDate());
        dto.setEndDate(eventDetail.getEndDate());
        dto.setMainCategoryId(eventDetail.getMainCategory() != null ? eventDetail.getMainCategory().getGroupId() : null);
        dto.setSubCategoryId(eventDetail.getSubCategory() != null ? eventDetail.getSubCategory().getCategoryId() : null);
        dto.setRegionCodeId(eventDetail.getRegionCode() != null ? eventDetail.getRegionCode().getRegionCodeId() : null);
        dto.setReentryAllowed(eventDetail.getReentryAllowed());
        dto.setCheckInAllowed(eventDetail.getCheckInAllowed());
        dto.setCheckOutAllowed(eventDetail.getCheckOutAllowed());
        dto.setHostCompany(eventDetail.getHostCompany());
        dto.setAge(eventDetail.getAge());
        return dto;
    }

    @Transactional
    public EventDetailModificationRequest createVersionRestoreRequest(Long eventId, Integer targetVersionNumber, Long requestedBy) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        if (modificationRequestRepository.existsPendingRequestByEventId(eventId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 처리 대기 중인 수정 요청이 존재합니다.");
        }

        // 대상 버전 확인
        EventVersion targetVersion = eventVersionService.getEventVersion(eventId, targetVersionNumber);
        EventSnapshotDto targetSnapshot = targetVersion.getSnapshotAsDto();

        // 현재 최신 버전과 동일한지 확인
        EventVersion latestVersion = eventVersionService.getEventVersions(eventId, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0);
        if (latestVersion.getVersionNumber().equals(targetVersionNumber)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 최신 버전입니다.");
        }

        UpdateStatusCode pendingStatus = updateStatusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "대기 상태 코드를 찾을 수 없습니다."));

        log.info("버전 복구 요청: from {} -> to {}", latestVersion.getVersionNumber(), targetVersionNumber);

        // 스냅샷을 ModificationDto로 변환
        EventDetailModificationDto modificationDto = convertSnapshotToModificationDto(targetSnapshot);

        // 현재 상태를 originalData로 설정
        EventDetailModificationDto currentDto = convertEventDetailToDto(event.getEventDetail());

        EventDetailModificationRequest request = new EventDetailModificationRequest();
        request.setEvent(event);
        request.setRequestedBy(requestedBy);
        request.setStatus(pendingStatus);
        request.setOriginalDataFromDto(currentDto);
        request.setModifiedDataFromDto(modificationDto);
        request.setAdminComment("버전 " + targetVersionNumber + "로 복구 요청");

        return modificationRequestRepository.save(request);
    }

    private EventDetailModificationDto convertSnapshotToModificationDto(EventSnapshotDto snapshot) {
        EventDetailModificationDto dto = new EventDetailModificationDto();

        // 기본 행사 정보
        dto.setTitleKr(snapshot.getTitleKr());
        dto.setTitleEng(snapshot.getTitleEng());
        
        // 위치 정보
        dto.setLocationId(snapshot.getLocationId());
        dto.setAddress(snapshot.getAddress());
        dto.setPlaceName(snapshot.getPlaceName());
        dto.setLatitude(snapshot.getLatitude());
        dto.setLongitude(snapshot.getLongitude());
        dto.setPlaceUrl(snapshot.getPlaceUrl());
        dto.setLocationDetail(snapshot.getLocationDetail());
        
        // 날짜 정보
        dto.setStartDate(snapshot.getStartDate());
        dto.setEndDate(snapshot.getEndDate());
        
        // 주최자 정보
        dto.setHostName(snapshot.getHostName());
        dto.setHostCompany(snapshot.getHostCompany());
        dto.setContactInfo(snapshot.getContactInfo());
        dto.setOfficialUrl(snapshot.getOfficialUrl());
        
        // 행사 상세 정보
        dto.setBio(snapshot.getBio());
        dto.setContent(snapshot.getContent());
        dto.setPolicy(snapshot.getPolicy());
        dto.setEventTime(snapshot.getEventTime());
        dto.setAge(snapshot.getAge());
        
        // 이미지 정보
        dto.setThumbnailUrl(snapshot.getThumbnailUrl());
        dto.setBannerUrl(snapshot.getBannerUrl());
        
        // 카테고리 정보
        dto.setMainCategoryId(snapshot.getMainCategoryId());
        dto.setMainCategoryName(snapshot.getMainCategoryName());
        dto.setSubCategoryId(snapshot.getSubCategoryId());
        dto.setSubCategoryName(snapshot.getSubCategoryName());
        dto.setRegionCodeId(snapshot.getRegionCodeId());
        
        // 체크인/체크아웃 설정
        dto.setCheckInAllowed(snapshot.getCheckInAllowed());
        dto.setCheckOutAllowed(snapshot.getCheckOutAllowed());
        dto.setReentryAllowed(snapshot.getReentryAllowed());
        
        // 매니저 정보
        dto.setBusinessNumber(snapshot.getBusinessNumber());
        dto.setManagerName(snapshot.getManagerName());
        dto.setManagerPhone(snapshot.getManagerPhone());
        dto.setManagerEmail(snapshot.getManagerEmail());
        
        // 외부 링크 변환
        if (snapshot.getExternalLinks() != null) {
            List<ExternalLinkRequestDto> externalLinks = snapshot.getExternalLinks().stream()
                    .map(link -> {
                        ExternalLinkRequestDto linkDto = new ExternalLinkRequestDto();
                        linkDto.setUrl(link.getUrl());
                        linkDto.setDisplayText(link.getDisplayText());
                        return linkDto;
                    })
                    .toList();
            dto.setExternalLinks(externalLinks);
        }

        return dto;
    }

    /**
     * content 내의 임시 URL을 영구 URL로 치환
     */
    private String replaceUrlsInContent(String content, Map<String, String> urlMappings) {
        if (content == null || content.isEmpty() || urlMappings.isEmpty()) {
            return content;
        }

        String updatedContent = content;
        for (Map.Entry<String, String> mapping : urlMappings.entrySet()) {
            String tempUrl = mapping.getKey();
            String permanentUrl = mapping.getValue();
            
            // HTML img 태그의 src 속성에서 URL 치환
            updatedContent = updatedContent.replace(tempUrl, permanentUrl);
            
            log.info("URL 치환: {} -> {}", tempUrl, permanentUrl);
        }

        return updatedContent;
    }
}