package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.EventDetailModificationDto;
import com.fairing.fairplay.event.dto.EventDetailModificationResponseDto;
import com.fairing.fairplay.event.dto.ModificationApprovalRequestDto;
import com.fairing.fairplay.event.entity.EventDetailModificationRequest;
import com.fairing.fairplay.event.service.EventDetailModificationRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventDetailModificationController {

    private final EventDetailModificationRequestService modificationRequestService;

    // 수정 요청
    @PostMapping("/{eventId}/modification-request")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    @FunctionAuth("createModificationRequest")
    public ResponseEntity<EventDetailModificationResponseDto> createModificationRequest(
            @PathVariable Long eventId,
            @RequestBody EventDetailModificationDto requestDto,
            @AuthenticationPrincipal CustomUserDetails auth) {

        EventDetailModificationDto modificationDto = new EventDetailModificationDto();

        // null이 아닌(실제 변경 요청된) 필드만 설정
        if (requestDto.getLocationId() != null) modificationDto.setLocationId(requestDto.getLocationId());
        if (requestDto.getLocationDetail() != null) modificationDto.setLocationDetail(requestDto.getLocationDetail());
        if (requestDto.getHostName() != null) modificationDto.setHostName(requestDto.getHostName());
        if (requestDto.getContactInfo() != null) modificationDto.setContactInfo(requestDto.getContactInfo());
        if (requestDto.getBio() != null) modificationDto.setBio(requestDto.getBio());
        if (requestDto.getContent() != null) modificationDto.setContent(requestDto.getContent());
        if (requestDto.getPolicy() != null) modificationDto.setPolicy(requestDto.getPolicy());
        if (requestDto.getOfficialUrl() != null) modificationDto.setOfficialUrl(requestDto.getOfficialUrl());
        if (requestDto.getEventTime() != null) modificationDto.setEventTime(requestDto.getEventTime());
        if (requestDto.getThumbnailUrl() != null) modificationDto.setThumbnailUrl(requestDto.getThumbnailUrl());
        if (requestDto.getBannerUrl() != null) modificationDto.setBannerUrl(requestDto.getBannerUrl());
        if (requestDto.getStartDate() != null) modificationDto.setStartDate(requestDto.getStartDate());
        if (requestDto.getEndDate() != null) modificationDto.setEndDate(requestDto.getEndDate());
        if (requestDto.getMainCategoryId() != null) modificationDto.setMainCategoryId(requestDto.getMainCategoryId());
        if (requestDto.getSubCategoryId() != null) modificationDto.setSubCategoryId(requestDto.getSubCategoryId());
        if (requestDto.getRegionCodeId() != null) modificationDto.setRegionCodeId(requestDto.getRegionCodeId());
        if (requestDto.getReentryAllowed() != null) modificationDto.setReentryAllowed(requestDto.getReentryAllowed());
        if (requestDto.getCheckOutAllowed() != null) modificationDto.setCheckOutAllowed(requestDto.getCheckOutAllowed());
        if (requestDto.getHostCompany() != null) modificationDto.setHostCompany(requestDto.getHostCompany());
        if (requestDto.getAge() != null) modificationDto.setAge(requestDto.getAge());

        EventDetailModificationRequest request = modificationRequestService.createModificationRequest(
                eventId, modificationDto, auth.getUserId());

        EventDetailModificationResponseDto responseDto = EventDetailModificationResponseDto.from(request);

        return ResponseEntity.ok(responseDto);
    }

    // 특정 행사의 대기 중인 수정 요청 조회
    @GetMapping("/{eventId}/modification-request")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    @FunctionAuth("getPendingModificationRequest")
    public ResponseEntity<EventDetailModificationResponseDto> getPendingModificationRequest(
            @PathVariable Long eventId) {

        Optional<EventDetailModificationRequest> pendingRequest =
                modificationRequestService.getPendingRequestByEventId(eventId);

        if (pendingRequest.isPresent()) {
            EventDetailModificationResponseDto responseDto = EventDetailModificationResponseDto.from(pendingRequest.get());
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/modification-requests")
    @PreAuthorize("hasAuthority('ADMIN')")
    @FunctionAuth("getModificationRequests")
    public ResponseEntity<Page<EventDetailModificationResponseDto>> getModificationRequests(
            @RequestParam(required = false) String status,           // PENDING, APPROVED, REJECTED
            @RequestParam(required = false) Long eventId,            // 특정 행사 필터링
            @RequestParam(required = false) Long requestedBy,        // 요청자 필터링
            Pageable pageable) {

        Page<EventDetailModificationRequest> requests = 
                modificationRequestService.getModificationRequests(status, eventId, requestedBy, pageable);

        Page<EventDetailModificationResponseDto> responsePage = 
                requests.map(EventDetailModificationResponseDto::from);

        return ResponseEntity.ok(responsePage);
    }

    @PutMapping("/modification-requests/{requestId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @FunctionAuth("processModificationRequest")
    public ResponseEntity<String> processModificationRequest(
            @PathVariable Long requestId,
            @RequestBody ModificationApprovalRequestDto approvalDto,
            @AuthenticationPrincipal CustomUserDetails auth) {

        if ("approve".equals(approvalDto.getAction())) {
            modificationRequestService.approveModificationRequest(requestId, auth.getUserId(), approvalDto.getAdminComment());
            return ResponseEntity.ok("수정 요청이 승인되었습니다.");
        } else if ("reject".equals(approvalDto.getAction())) {
            modificationRequestService.rejectModificationRequest(requestId, auth.getUserId(), approvalDto.getAdminComment());
            return ResponseEntity.ok("수정 요청이 반려되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("유효하지 않은 액션입니다.");
        }
    }
}