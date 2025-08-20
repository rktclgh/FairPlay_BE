package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    // 1단계: 티켓 환불 요청 (구매자가 환불 요청)
    @PostMapping("/{paymentId}/request")
    public ResponseEntity<PaymentResponseDto> requestRefund(
            @PathVariable Long paymentId,
            @RequestBody PaymentRequestDto paymentRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        PaymentResponseDto refundRequest = refundService.requestRefund(paymentId, paymentRequestDto, userDetails.getUserId());
        return ResponseEntity.ok(refundRequest);
    }

    // 2단계: 티켓 환불 승인 (관리자가 환불 승인)
    @PostMapping("/{refundId}/approve")
    @FunctionAuth("approveRefund")
    public ResponseEntity<PaymentResponseDto> approveRefund(
            @PathVariable Long refundId,
            @RequestBody RefundApprovalDto approval,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        PaymentResponseDto approvedRefund = refundService.approveRefund(refundId, approval, userDetails.getUserId());
        return ResponseEntity.ok(approvedRefund);
    }

    // 티켓 환불 거절 (관리자가 환불 거절)
    @PostMapping("/{refundId}/reject")
    @FunctionAuth("rejectRefund")
    public ResponseEntity<PaymentResponseDto> rejectRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) String rejectReason,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        PaymentResponseDto rejectedRefund = refundService.rejectRefund(refundId, rejectReason, userDetails.getUserId());
        return ResponseEntity.ok(rejectedRefund);
    }

    // 환불 요청 목록 조회 (관리자용)
    @GetMapping
    @FunctionAuth("getAllRefunds")
    public ResponseEntity<List<RefundResponseDto>> getAllRefunds(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RefundResponseDto> refunds = refundService.getAllRefunds(eventId, userDetails);
        return ResponseEntity.ok(refunds);
    }

    // 내 환불 요청 목록 조회 (구매자용)
    @GetMapping("/me")
    public ResponseEntity<List<RefundListResponseDto>> getMyRefunds(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Long userId = userDetails.getUserId();
        List<RefundListResponseDto> myRefunds = refundService.getMyRefundList(userId);
        return ResponseEntity.ok(myRefunds);
    }

    // 대기 중인 환불 요청 조회 (관리자용)
    @GetMapping("/pending")
    @FunctionAuth("getPendingRefunds")
    public ResponseEntity<List<RefundResponseDto>> getPendingRefunds(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        List<RefundResponseDto> pendingRefunds = refundService.getPendingRefunds(eventId, userDetails);
        return ResponseEntity.ok(pendingRefunds);
    }

    // 환불 목록 조회 (필터링 및 페이징 지원)
    @PostMapping("/list")
    // @FunctionAuth("getRefundList") // 임시로 주석 처리
    public ResponseEntity<Page<RefundListResponseDto>> getRefundList(
            @RequestBody RefundListRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Page<RefundListResponseDto> refundList = refundService.getRefundList(request);
        return ResponseEntity.ok(refundList);
    }

    // 환불 요청 (새로운 DTO 사용)
    @PostMapping("/request-refund")
    public ResponseEntity<PaymentResponseDto> requestRefundWithDto(
            @RequestBody RefundRequestDto refundRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        PaymentResponseDto refundResponse = refundService.requestRefundWithDto(refundRequest, userDetails.getUserId());
        return ResponseEntity.ok(refundResponse);
    }
}
