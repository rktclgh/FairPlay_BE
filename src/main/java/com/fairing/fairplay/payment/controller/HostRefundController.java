package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.service.EventManagerService;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.service.AdminRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/host/refunds")
@RequiredArgsConstructor
public class HostRefundController {

    private final AdminRefundService adminRefundService; // 기존 서비스 재사용
    private final EventManagerService eventManagerService;

    /**
     * 호스트(행사 관리자)용 환불 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<AdminRefundListResponseDto>> getHostRefundList(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String paymentDateFrom,
            @RequestParam(required = false) String paymentDateTo,
            @RequestParam(required = false) String refundStatus,
            @RequestParam(required = false) String paymentTargetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        // EVENT_MANAGER 권한 검증
        validateEventManagerAccess(userDetails);
        
        // 요청된 이벤트가 본인 관리 이벤트인지 검증
        if (eventId != null) {
            validateEventAccess(eventId, userDetails);
        }
        
        // 관리 가능한 결제 타입으로 제한 (RESERVATION, BOOTH만)
        List<String> allowedTypes = eventManagerService.getAllowedPaymentTargetTypes();
        if (paymentTargetType != null && !allowedTypes.contains(paymentTargetType)) {
            throw new IllegalArgumentException("관리 권한이 없는 결제 타입입니다: " + paymentTargetType);
        }
        
        AdminRefundListRequestDto request = AdminRefundListRequestDto.builder()
                .eventId(eventId)
                .userName(userName)
                .paymentDateFrom(paymentDateFrom)
                .paymentDateTo(paymentDateTo)
                .refundStatus(refundStatus)
                .paymentTargetType(paymentTargetType)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        Page<AdminRefundListResponseDto> result = adminRefundService.getAdminRefundList(request, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 호스트용 환불 승인 처리
     */
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<RefundResponseDto> approveRefundAsHost(
            @PathVariable Long refundId,
            @RequestBody RefundApprovalDto approval,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        validateEventManagerAccess(userDetails);
        
        // 해당 환불이 본인 관리 이벤트 소속인지 검증
        validateRefundOwnership(refundId, userDetails);
        
        RefundResponseDto result = adminRefundService.approveRefund(refundId, approval, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 호스트용 환불 거부 처리
     */
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<RefundResponseDto> rejectRefundAsHost(
            @PathVariable Long refundId,
            @RequestBody RefundApprovalDto rejection,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        validateEventManagerAccess(userDetails);
        
        // 해당 환불이 본인 관리 이벤트 소속인지 검증
        validateRefundOwnership(refundId, userDetails);
        
        RefundResponseDto result = adminRefundService.rejectRefund(refundId, rejection, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 호스트가 관리하는 이벤트 목록 조회
     */
    @GetMapping("/managed-events")
    public ResponseEntity<List<ManagedEventDto>> getManagedEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        validateEventManagerAccess(userDetails);
        
        List<ManagedEventDto> managedEvents = eventManagerService.getManagedEvents(userDetails.getUserId())
                .stream()
                .map(event -> ManagedEventDto.builder()
                        .eventId(event.getEventId())
                        .eventName(event.getTitleKr())
                        .eventStatus(event.getStatusCode() != null ? event.getStatusCode().getCode() : null)
                        .startDate(event.getEventDetail() != null ? event.getEventDetail().getStartDate() : null)
                        .endDate(event.getEventDetail() != null ? event.getEventDetail().getEndDate() : null)
                        .build())
                .toList();
        
        return ResponseEntity.ok(managedEvents);
    }

    /**
     * EVENT_MANAGER 권한 검증
     */
    private void validateEventManagerAccess(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        String roleCode = userDetails.getRoleCode();
        if (!"EVENT_MANAGER".equals(roleCode) && !"ADMIN".equals(roleCode)) {
            throw new IllegalArgumentException("행사 관리자 권한이 없습니다.");
        }
    }

    /**
     * 특정 이벤트에 대한 접근 권한 검증
     */
    private void validateEventAccess(Long eventId, CustomUserDetails userDetails) {
        if ("ADMIN".equals(userDetails.getRoleCode())) {
            return; // 전체 관리자는 모든 이벤트 접근 가능
        }
        
        if (!eventManagerService.canManageEvent(eventId, userDetails.getUserId())) {
            throw new IllegalArgumentException("해당 이벤트의 관리 권한이 없습니다: " + eventId);
        }
    }

    /**
     * 환불 요청에 대한 소유권 검증
     */
    private void validateRefundOwnership(Long refundId, CustomUserDetails userDetails) {
        if ("ADMIN".equals(userDetails.getRoleCode())) {
            return; // 전체 관리자는 모든 환불 처리 가능
        }
        
        if (!eventManagerService.isRefundInManagedEvent(refundId, userDetails.getUserId())) {
            throw new IllegalArgumentException("해당 환불 요청에 대한 처리 권한이 없습니다: " + refundId);
        }
    }
}