package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    // 특정 결제의 특정 상태 환불 목록 조회 (FK 기반)
    List<Refund> findByPaymentAndRefundStatusCode(Payment payment, RefundStatusCode refundStatusCode);

    // 특정 결제의 모든 환불 목록 조회
    List<Refund> findByPayment(Payment payment);

    // 특정 사용자의 환불 목록 조회
    @Query("SELECT r FROM Refund r WHERE r.payment.user.userId = :userId")
    List<Refund> findByUserId(@Param("userId") Long userId);

    // 특정 이벤트의 환불 목록 조회
    @Query("SELECT r FROM Refund r WHERE r.payment.event.eventId = :eventId")
    List<Refund> findByEventId(@Param("eventId") Long eventId);

    // 특정 상태의 환불 목록 조회 (FK 기반)
    List<Refund> findByRefundStatusCode(RefundStatusCode refundStatusCode);

    // 특정 이벤트의 특정 상태 환불 목록 조회 (FK 기반)
    @Query("SELECT r FROM Refund r WHERE r.payment.event.eventId = :eventId AND r.refundStatusCode = :refundStatusCode")
    List<Refund> findByEventIdAndRefundStatusCode(@Param("eventId") Long eventId, @Param("refundStatusCode") RefundStatusCode refundStatusCode);

}
