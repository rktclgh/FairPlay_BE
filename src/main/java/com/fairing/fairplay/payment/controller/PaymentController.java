package com.fairing.fairplay.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.dto.PaymentResponseDto;
import com.fairing.fairplay.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 요청 정보 저장
    @PostMapping("/request")
    @FunctionAuth("requestPayment")
    public ResponseEntity<PaymentResponseDto> requestPayment(@RequestBody PaymentRequestDto paymentRequestDto,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        PaymentResponseDto savedPayment = paymentService.savePayment(paymentRequestDto, userDetails.getUserId());
        return ResponseEntity.ok(savedPayment);
    }

    // 결제 완료 처리 (PG사 결제 후 호출)
    @PostMapping("/complete")
    @FunctionAuth("completePayment")
    public ResponseEntity<PaymentResponseDto> completePayment(@RequestBody PaymentRequestDto paymentRequestDto) {
        PaymentResponseDto completedPayment = paymentService.completePayment(paymentRequestDto);
        return ResponseEntity.ok(completedPayment);
    }

    // 결제 전체 조회 (전체 관리자, 행사 관리자)
    @GetMapping
    @FunctionAuth("getAllPayments")
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        List<PaymentResponseDto> payments = paymentService.getAllPayments(eventId, userDetails);
        return ResponseEntity.ok(payments);
    }

    // 나의 결제 목록 조회 (예약/부스/광고 전체)
    @GetMapping("/me")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        Long userId = userDetails.getUserId();
        List<PaymentResponseDto> myPayments = paymentService.getMyPayments(userId);
        return ResponseEntity.ok(myPayments);
    }

    // merchantUid 생성 API
    @GetMapping("/merchant-uid")
    public ResponseEntity<String> generateMerchantUid(@RequestParam String targetType) {
        String merchantUid = paymentService.generateMerchantUid(targetType);
        return ResponseEntity.ok(merchantUid);
    }

    // 결제 target_id 업데이트 API
    @PutMapping("/{merchantUid}/target-id")
    @FunctionAuth("updatePaymentTargetId")
    public ResponseEntity<Void> updatePaymentTargetId(
            @PathVariable String merchantUid,
            @RequestBody TargetIdUpdateRequest request) {
        paymentService.updatePaymentTargetId(merchantUid, request.getTargetId());
        return ResponseEntity.ok().build();
    }

    // 내부 DTO 클래스
    public static class TargetIdUpdateRequest {
        private Long targetId;
        
        public Long getTargetId() {
            return targetId;
        }
        
        public void setTargetId(Long targetId) {
            this.targetId = targetId;
        }
    }

}