package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.dto.PaymentSearchCriteria;
import com.fairing.fairplay.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaymentAdminRepository {

    /**
     * 검색 조건과 권한에 따른 결제 내역 조회 (페이징)
     */
    Page<Payment> findPaymentsWithCriteria(PaymentSearchCriteria criteria, Long managerId, Pageable pageable);

    /**
     * 권한 검증을 포함한 결제 상세 조회
     */
    Optional<Payment> findPaymentWithPermissionCheck(Long paymentId, Long managerId);

    /**
     * 엑셀 다운로드용 결제 내역 조회 (페이징 없음)
     */
    List<Payment> findPaymentsForExport(PaymentSearchCriteria criteria, Long managerId);

    /**
     * 결제 건수 통계 조회
     */
    Map<String, Long> getPaymentCountStatistics(PaymentSearchCriteria criteria, Long managerId);

    /**
     * 결제 금액 통계 조회
     */
    Map<String, BigDecimal> getPaymentAmountStatistics(PaymentSearchCriteria criteria, Long managerId);

    /**
     * 결제 타입별 통계 조회
     */
    Map<String, Object> getPaymentTypeStatistics(PaymentSearchCriteria criteria, Long managerId);
}