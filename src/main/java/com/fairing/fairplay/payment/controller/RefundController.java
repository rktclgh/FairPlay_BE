package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
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
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: userId = 1L 사용자로 가정
        // if(userDetails == null) {
        //     throw new IllegalArgumentException("사용자 인증이 필요합니다.");
        // }
        
        PaymentResponseDto refundRequest = refundService.requestRefund(paymentId, paymentRequestDto);
        return ResponseEntity.ok(refundRequest);
    }

    // 2단계: 티켓 환불 승인 (관리자가 환불 승인)
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<PaymentResponseDto> approveRefund(
            @PathVariable Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: ADMIN 가정
        // if(userDetails == null) {
        //     throw new IllegalArgumentException("사용자 인증이 필요합니다.");
        // }
        
        PaymentResponseDto approvedRefund = refundService.approveRefund(refundId);
        return ResponseEntity.ok(approvedRefund);
    }

    // 티켓 환불 거절 (관리자가 환불 거절)
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<PaymentResponseDto> rejectRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) String rejectReason,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: ADMIN 가정
        // if(userDetails == null) {
        //     throw new IllegalArgumentException("사용자 인증이 필요합니다.");
        // }
        
        PaymentResponseDto rejectedRefund = refundService.rejectRefund(refundId, rejectReason);
        return ResponseEntity.ok(rejectedRefund);
    }

    // 환불 요청 목록 조회 (관리자용)
    @GetMapping
    public ResponseEntity<List<RefundResponseDto>> getAllRefunds(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RefundResponseDto> refunds = refundService.getAllRefunds(eventId, userDetails);
        return ResponseEntity.ok(refunds);
    }

    // 내 환불 요청 목록 조회 (구매자용)
    @GetMapping("/me")
    public ResponseEntity<List<RefundResponseDto>> getMyRefunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: userId = 1L
        Long userId = 1L;
        // if(userDetails != null) { userId = userDetails.getUserId(); }
        
        List<RefundResponseDto> myRefunds = refundService.getMyRefunds(userId);
        return ResponseEntity.ok(myRefunds);
    }

    // 대기 중인 환불 요청 조회 (관리자용)
    @GetMapping("/pending")
    public ResponseEntity<List<RefundResponseDto>> getPendingRefunds(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: ADMIN 가정
        // if(userDetails == null) {
        //     throw new IllegalArgumentException("사용자 인증이 필요합니다.");
        // }
        
        List<RefundResponseDto> pendingRefunds = refundService.getPendingRefunds(eventId);
        return ResponseEntity.ok(pendingRefunds);
    }
}
