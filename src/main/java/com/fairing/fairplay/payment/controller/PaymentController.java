package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 요청 정보 저장 (예약/부스/광고 통합)
    @PostMapping("/request")
    public ResponseEntity<PaymentResponseDto> requestPayment(@RequestBody PaymentRequestDto paymentRequestDto,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 임시 하드코딩: userId = 1L
        Long userId = 1L;
        // if(userDetails != null) { userId = userDetails.getUserId(); }
        
        PaymentResponseDto savedPayment = paymentService.savePayment(paymentRequestDto, userId);
        return ResponseEntity.ok(savedPayment);
    }

    // 결제 완료 처리 (PG사 결제 후 호출)
    @PostMapping("/complete")
    public ResponseEntity<PaymentResponseDto> completePayment(@RequestBody PaymentRequestDto paymentRequestDto) {
        PaymentResponseDto completedPayment = paymentService.completePayment(paymentRequestDto);
        return ResponseEntity.ok(completedPayment);
    }

    // 결제 전체 조회 (전체 관리자, 행사 관리자)
    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 임시 하드코딩: ADMIN 사용자로 설정
        // CustomUserDetails mockUser = CustomUserDetails.builder().userId(1L).roleCode("ADMIN").build();
        List<PaymentResponseDto> payments = paymentService.getAllPayments(eventId, userDetails);
        return ResponseEntity.ok(payments);
    }

    // 나의 결제 목록 조회 (예약/부스/광고 전체)
    @GetMapping("/me")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 임시 하드코딩: userId = 1L
        Long userId = 1L;
        // if(userDetails != null) { userId = userDetails.getUserId(); }
        
        List<PaymentResponseDto> myPayments = paymentService.getMyPayments(userId);
        return ResponseEntity.ok(myPayments);
    }

}