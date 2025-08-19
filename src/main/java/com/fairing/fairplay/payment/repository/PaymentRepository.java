package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMerchantUid(String merchantUid);

    List<Payment> findByEvent_EventId(Long eventId);

    List<Payment> findByUser_UserId(Long userId);

    // 사용자별 결제 내역 조회 (이벤트 정보 포함)
    @Query("SELECT p FROM Payment p " +
           "LEFT JOIN FETCH p.event e " +
           "JOIN FETCH p.user u " +
           "JOIN FETCH p.paymentTargetType ptt " +
           "JOIN FETCH p.paymentTypeCode ptc " +
           "JOIN FETCH p.paymentStatusCode psc " +
           "WHERE p.user.userId = :userId " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findByUserIdWithEventInfo(@Param("userId") Long userId);

    boolean existsByTargetIdAndPaymentTargetType_PaymentTargetCodeAndPaymentStatusCode_Code(Long targetId, String paymentTargetType, String paymentStatusCode);

    // imp_uid 중복 검증용
    boolean existsByImpUidAndPaymentStatusCode_Code(String impUid, String paymentStatusCode);
    
    // 특정 target_id와 payment_target_type으로 결제 정보 조회
    Optional<Payment> findByTargetIdAndPaymentTargetType_PaymentTargetCode(Long targetId, String paymentTargetCode);
    
    // 특정 target_id와 payment_target_type으로 모든 결제 정보 조회 (부스 취소 시 사용)
    List<Payment> findByPaymentTargetType_PaymentTargetCodeAndTargetId(String paymentTargetCode, Long targetId);
}
