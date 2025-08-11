package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.core.util.JsonUtil;
import com.fairing.fairplay.event.dto.EventDetailModificationDto;
import com.fairing.fairplay.event.dto.EventDetailRequestDto;
import com.fairing.fairplay.event.dto.EventSnapshotDto;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventDetailModificationRequestService {

    private final EventDetailModificationRequestRepository modificationRequestRepository;
    private final UpdateStatusCodeRepository updateStatusCodeRepository;
    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final LocationRepository locationRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final EventVersionService eventVersionService;
    private final AwsS3Service awsS3Service;

    public EventDetailModificationRequestService(EventDetailModificationRequestRepository modificationRequestRepository, UpdateStatusCodeRepository updateStatusCodeRepository, EventRepository eventRepository, EventDetailRepository eventDetailRepository, LocationRepository locationRepository, MainCategoryRepository mainCategoryRepository, SubCategoryRepository subCategoryRepository, RegionCodeRepository regionCodeRepository, EventVersionService eventVersionService, AwsS3Service awsS3Service) {
        this.modificationRequestRepository = modificationRequestRepository;
        this.updateStatusCodeRepository = updateStatusCodeRepository;
        this.eventRepository = eventRepository;
        this.eventDetailRepository = eventDetailRepository;
        this.locationRepository = locationRepository;
        this.mainCategoryRepository = mainCategoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.regionCodeRepository = regionCodeRepository;
        this.eventVersionService = eventVersionService;
        this.awsS3Service = awsS3Service;
    }

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
        if (modificationDto.getTempFiles() != null && !modificationDto.getTempFiles().isEmpty()) {
            for (EventDetailRequestDto.FileUploadDto fileDto : modificationDto.getTempFiles()) {
                try {
                    String directory = "events/" + eventId + "/" + fileDto.getUsage();
                    String newKey = awsS3Service.moveToPermanent(fileDto.getS3Key(), directory);
                    String cdnUrl = awsS3Service.getCdnUrl(newKey);
                    newFileKeys.add(newKey);

                    switch (fileDto.getUsage().toLowerCase()) {
                        case "banner":
                            modificationDto.setBannerUrl(cdnUrl);
                            break;
                        case "thumbnail":
                            modificationDto.setThumbnailUrl(cdnUrl);
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing temporary file for modification request: {}", e.getMessage());
                    throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다: " + fileDto.getOriginalFileName());
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

    private void applyModificationToEventDetail(EventDetailModificationRequest request) {
        EventDetail eventDetail = request.getEvent().getEventDetail();
        EventDetailModificationDto modifiedData = request.getModifiedDataAsDto();

        if (modifiedData.getLocationId() != null) {
            Location location = locationRepository.findById(modifiedData.getLocationId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "위치 정보를 찾을 수 없습니다."));
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

        if (modifiedData.getRegionCodeId() != null) {
            RegionCode regionCode = regionCodeRepository.findById(modifiedData.getRegionCodeId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "지역 코드를 찾을 수 없습니다."));
            eventDetail.setRegionCode(regionCode);
        }

        // null이 아닌(변경 요청된) 필드만 업데이트
        if (modifiedData.getLocationDetail() != null) eventDetail.setLocationDetail(modifiedData.getLocationDetail());
        if (modifiedData.getHostName() != null) eventDetail.setHostName(modifiedData.getHostName());
        if (modifiedData.getContactInfo() != null) eventDetail.setContactInfo(modifiedData.getContactInfo());
        if (modifiedData.getBio() != null) eventDetail.setBio(modifiedData.getBio());
        if (modifiedData.getContent() != null) eventDetail.setContent(modifiedData.getContent());
        if (modifiedData.getPolicy() != null) eventDetail.setPolicy(modifiedData.getPolicy());
        if (modifiedData.getOfficialUrl() != null) eventDetail.setOfficialUrl(modifiedData.getOfficialUrl());
        if (modifiedData.getEventTime() != null) eventDetail.setEventTime(modifiedData.getEventTime());
        if (modifiedData.getThumbnailUrl() != null) eventDetail.setThumbnailUrl(modifiedData.getThumbnailUrl());
        if (modifiedData.getBannerUrl() != null) eventDetail.setBannerUrl(modifiedData.getBannerUrl());
        if (modifiedData.getStartDate() != null) eventDetail.setStartDate(modifiedData.getStartDate());
        if (modifiedData.getEndDate() != null) eventDetail.setEndDate(modifiedData.getEndDate());
        if (modifiedData.getReentryAllowed() != null) eventDetail.setReentryAllowed(modifiedData.getReentryAllowed());
        if (modifiedData.getCheckOutAllowed() != null) eventDetail.setCheckOutAllowed(modifiedData.getCheckOutAllowed());

        eventDetailRepository.save(eventDetail);
    }

    private EventDetailModificationDto convertEventDetailToDto(EventDetail eventDetail) {
        EventDetailModificationDto dto = new EventDetailModificationDto();
        dto.setLocationId(eventDetail.getLocation() != null ? eventDetail.getLocation().getLocationId() : null);
        dto.setLocationDetail(eventDetail.getLocationDetail());
        dto.setHostName(eventDetail.getHostName());
        dto.setContactInfo(eventDetail.getContactInfo());
        dto.setBio(eventDetail.getBio());
        dto.setContent(eventDetail.getContent());
        dto.setPolicy(eventDetail.getPolicy());
        dto.setOfficialUrl(eventDetail.getOfficialUrl());
        dto.setEventTime(eventDetail.getEventTime());
        dto.setThumbnailUrl(eventDetail.getThumbnailUrl());
        dto.setBannerUrl(eventDetail.getBannerUrl());
        dto.setStartDate(eventDetail.getStartDate());
        dto.setEndDate(eventDetail.getEndDate());
        dto.setMainCategoryId(eventDetail.getMainCategory() != null ? eventDetail.getMainCategory().getGroupId() : null);
        dto.setSubCategoryId(eventDetail.getSubCategory() != null ? eventDetail.getSubCategory().getCategoryId() : null);
        dto.setRegionCodeId(eventDetail.getRegionCode() != null ? eventDetail.getRegionCode().getRegionCodeId() : null);
        dto.setReentryAllowed(eventDetail.getReentryAllowed());
        dto.setCheckOutAllowed(eventDetail.getCheckOutAllowed());
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

        dto.setLocationId(snapshot.getLocationId());
        dto.setLocationDetail(snapshot.getLocationDetail());
        dto.setHostName(snapshot.getHostName());
        dto.setContactInfo(snapshot.getContactInfo());
        dto.setBio(snapshot.getBio());
        dto.setContent(snapshot.getContent());
        dto.setPolicy(snapshot.getPolicy());
        dto.setOfficialUrl(snapshot.getOfficialUrl());
        dto.setEventTime(snapshot.getEventTime());
        dto.setThumbnailUrl(snapshot.getThumbnailUrl());
        dto.setBannerUrl(snapshot.getBannerUrl());
        dto.setStartDate(snapshot.getStartDate());
        dto.setEndDate(snapshot.getEndDate());
        dto.setMainCategoryId(snapshot.getMainCategoryId());
        dto.setSubCategoryId(snapshot.getSubCategoryId());
        dto.setRegionCodeId(snapshot.getRegionCodeId());
        dto.setReentryAllowed(snapshot.getReentryAllowed());
        dto.setCheckOutAllowed(snapshot.getCheckOutAllowed());

        return dto;
    }
}