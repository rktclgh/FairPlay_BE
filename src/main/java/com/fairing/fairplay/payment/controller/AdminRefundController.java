package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.AdminRefundListRequestDto;
import com.fairing.fairplay.payment.dto.AdminRefundListResponseDto;
import com.fairing.fairplay.payment.dto.RefundApprovalDto;
import com.fairing.fairplay.payment.dto.RefundResponseDto;
import com.fairing.fairplay.payment.service.AdminRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    /**
     * 관리자용 환불 목록 조회
     */
    @GetMapping
    //@FunctionAuth("REFUND_MANAGE")
    public ResponseEntity<Page<AdminRefundListResponseDto>> getAdminRefundList(
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String paymentDateFrom,
            @RequestParam(required = false) String paymentDateTo,
            @RequestParam(required = false) String refundStatus,
            @RequestParam(required = false) String paymentTargetType,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        AdminRefundListRequestDto request = AdminRefundListRequestDto.builder()
                .eventName(eventName)
                .userName(userName)
                .paymentDateFrom(paymentDateFrom)
                .paymentDateTo(paymentDateTo)
                .refundStatus(refundStatus)
                .paymentTargetType(paymentTargetType)
                .eventId(eventId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        Page<AdminRefundListResponseDto> result = adminRefundService.getAdminRefundList(request, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 환불 승인 처리
     */
    @PostMapping("/{refundId}/approve")
    //@FunctionAuth("REFUND_APPROVE")
    public ResponseEntity<RefundResponseDto> approveRefund(
            @PathVariable Long refundId,
            @RequestBody RefundApprovalDto approval,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        RefundResponseDto result = adminRefundService.approveRefund(refundId, approval, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 환불 거부 처리
     */
    @PostMapping("/{refundId}/reject")
    //@FunctionAuth("REFUND_REJECT")
    public ResponseEntity<RefundResponseDto> rejectRefund(
            @PathVariable Long refundId,
            @RequestBody RefundApprovalDto rejection,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        RefundResponseDto result = adminRefundService.rejectRefund(refundId, rejection, userDetails);
        return ResponseEntity.ok(result);
    }

    /**
     * 환불 상세 정보 조회 (승인 모달용)
     */
    @GetMapping("/{refundId}")
    //@FunctionAuth("REFUND_VIEW")
    public ResponseEntity<AdminRefundListResponseDto> getRefundDetail(
            @PathVariable Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        
        // 단일 환불 조회를 위한 필터 설정
        AdminRefundListRequestDto request = AdminRefundListRequestDto.builder()
                .page(0)
                .size(1)
                .build();
        
        Page<AdminRefundListResponseDto> result = adminRefundService.getAdminRefundList(request, userDetails);
        
        AdminRefundListResponseDto refundDetail = result.getContent()
                .stream()
                .filter(refund -> refund.getRefundId().equals(refundId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("환불 정보를 찾을 수 없습니다: " + refundId));
        
        return ResponseEntity.ok(refundDetail);
    }
}