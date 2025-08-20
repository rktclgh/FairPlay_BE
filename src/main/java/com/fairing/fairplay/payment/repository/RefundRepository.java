package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.dto.AdminRefundListResponseDto;
import com.fairing.fairplay.payment.dto.RefundListResponseDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 환불 목록 조회 (필터링 및 페이징 지원)
    @Query("""
        SELECT new com.fairing.fairplay.payment.dto.RefundListResponseDto(
            r.refundId,
            p.paymentId,
            p.merchantUid,
            e.eventId,
            e.titleKr,
            u.userId,
            u.name,
            u.email,
            u.phone,
            ptt.paymentTargetCode,
            ptt.paymentTargetName,
            p.targetId,
            p.quantity,
            p.price,
            p.amount,
            p.paidAt,
            r.amount,
            r.reason,
            rsc.code,
            rsc.name,
            r.createdAt,
            r.approvedAt
        )
        FROM Refund r
        JOIN r.payment p
        LEFT JOIN p.event e
        JOIN p.user u
        JOIN p.paymentTargetType ptt
        JOIN r.refundStatusCode rsc
        WHERE (:eventName IS NULL OR e.titleKr LIKE %:eventName%)
        AND (:paymentDateFrom IS NULL OR p.paidAt >= :paymentDateFrom)
        AND (:paymentDateTo IS NULL OR p.paidAt <= :paymentDateTo)
        AND (:refundStatus IS NULL OR rsc.code = :refundStatus)
        AND (:paymentTargetType IS NULL OR ptt.paymentTargetCode = :paymentTargetType)
    """)
    Page<RefundListResponseDto> findRefundsWithFilters(
        @Param("eventName") String eventName,
        @Param("paymentDateFrom") LocalDateTime paymentDateFrom,
        @Param("paymentDateTo") LocalDateTime paymentDateTo,
        @Param("refundStatus") String refundStatus,
        @Param("paymentTargetType") String paymentTargetType,
        Pageable pageable
    );

    // 관리자용 환불 목록 조회 (상세 정보 포함)
    @Query("""
        SELECT new com.fairing.fairplay.payment.dto.AdminRefundListResponseDto(
            r.refundId,
            r.amount,
            r.reason,
            rsc.code,
            rsc.name,
            r.createdAt,
            r.approvedAt,
            r.adminComment,
            approver.name,
            p.paymentId,
            p.merchantUid,
            p.impUid,
            p.amount,
            p.quantity,
            p.price,
            ptt.paymentTargetCode,
            ptt.paymentTargetName,
            p.paidAt,
            e.eventId,
            e.titleKr,
            f.startDate,
            f.endDate,
            u.userId,
            u.name,
            u.email,
            u.phone
        )
        FROM Refund r
        JOIN r.payment p
        LEFT JOIN p.event e
        JOIN e.eventDetail f
        JOIN p.user u
        JOIN p.paymentTargetType ptt
        JOIN r.refundStatusCode rsc
        LEFT JOIN r.approvedBy approver
        WHERE (:eventName IS NULL OR e.titleKr LIKE %:eventName%)
        AND (:userName IS NULL OR u.name LIKE %:userName%)
        AND (:paymentDateFrom IS NULL OR p.paidAt >= :paymentDateFrom)
        AND (:paymentDateTo IS NULL OR p.paidAt <= :paymentDateTo)
        AND (:refundStatus IS NULL OR rsc.code = :refundStatus)
        AND (:paymentTargetType IS NULL OR ptt.paymentTargetCode = :paymentTargetType)
        AND (:eventId IS NULL OR e.eventId = :eventId)
    """)
    Page<AdminRefundListResponseDto> findAdminRefundsWithFilters(
        @Param("eventName") String eventName,
        @Param("userName") String userName,
        @Param("paymentDateFrom") LocalDateTime paymentDateFrom,
        @Param("paymentDateTo") LocalDateTime paymentDateTo,
        @Param("refundStatus") String refundStatus,
        @Param("paymentTargetType") String paymentTargetType,
        @Param("eventId") Long eventId,
        Pageable pageable
    );

}
