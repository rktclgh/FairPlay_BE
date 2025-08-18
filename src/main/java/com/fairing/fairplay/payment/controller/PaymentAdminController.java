package com.fairing.fairplay.payment.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.PaymentAdminDto;
import com.fairing.fairplay.payment.dto.PaymentSearchCriteria;
import com.fairing.fairplay.payment.service.PaymentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminService paymentAdminService;

    /**
     * 결제 내역 통합 조회 (관리자용)
     * - ADMIN: 모든 결제 내역 조회
     * - EVENT_MANAGER: 본인이 관리하는 행사의 결제 내역만 조회
     */
    @GetMapping
    @FunctionAuth("getPaymentList")
    public ResponseEntity<Map<String, Object>> getPaymentList(
            PaymentSearchCriteria criteria,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Page<PaymentAdminDto> paymentPage = paymentAdminService.getPaymentList(criteria, userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("payments", paymentPage.getContent());
        response.put("currentPage", paymentPage.getNumber());
        response.put("totalPages", paymentPage.getTotalPages());
        response.put("totalElements", paymentPage.getTotalElements());
        response.put("size", paymentPage.getSize());
        response.put("hasNext", paymentPage.hasNext());
        response.put("hasPrevious", paymentPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * 결제 상세 정보 조회 (관리자용)
     */
    @GetMapping("/{paymentId}")
    @FunctionAuth("getPaymentDetail")
    public ResponseEntity<PaymentAdminDto> getPaymentDetail(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        PaymentAdminDto paymentDetail = paymentAdminService.getPaymentDetail(paymentId, userDetails);
        return ResponseEntity.ok(paymentDetail);
    }

    /**
     * 결제 내역 엑셀 다운로드 (관리자용)
     */
    @GetMapping("/export/excel")
    @FunctionAuth("exportPaymentExcel")
    public ResponseEntity<byte[]> exportPaymentExcel(
            PaymentSearchCriteria criteria,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        byte[] excelData = paymentAdminService.exportPaymentExcel(criteria, userDetails);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=payment_list.xlsx")
                .body(excelData);
    }

    /**
     * 결제 통계 정보 조회 (관리자용)
     */
    @GetMapping("/statistics")
    @FunctionAuth("getPaymentStatistics")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(
            PaymentSearchCriteria criteria,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Map<String, Object> statistics = paymentAdminService.getPaymentStatistics(criteria, userDetails);
        return ResponseEntity.ok(statistics);
    }
}