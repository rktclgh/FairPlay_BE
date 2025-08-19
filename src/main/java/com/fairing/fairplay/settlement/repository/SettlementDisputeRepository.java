package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.entity.SettlementDispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementDisputeRepository extends JpaRepository<SettlementDispute, Long> {
    /**
     * 특정 정산에 대한 이의신청 존재 여부 확인
     */
    boolean existsBySettlement_SettlementId(Long settlementId);

    /**
     * 특정 정산에 대한 이의신청 조회
     */
    Optional<SettlementDispute> findBySettlement_SettlementId(Long settlementId);

    /**
     * 정산 정보와 함께 이의신청 목록 조회
     */
    @Query("SELECT d FROM SettlementDispute d " +
            "JOIN FETCH d.settlement s " +
            "ORDER BY d.submittedAt DESC")
    Page<SettlementDispute> findAllWithSettlement(Pageable pageable);

    /**
     * 상태별 이의신청 개수 조회
     */
    long countByStatus(SettlementDispute.DisputeProcessStatus status);

    /**
     * 특정 요청자의 이의신청 목록 조회
     */
    @Query("SELECT d FROM SettlementDispute d " +
            "JOIN FETCH d.settlement s " +
            "WHERE d.requesterId = :requesterId " +
            "ORDER BY d.submittedAt DESC")
    Page<SettlementDispute> findByRequesterIdWithSettlement(@Param("requesterId") Long requesterId, Pageable pageable);

    /**
     * 특정 기간 내 이의신청 조회
     */
    @Query("SELECT d FROM SettlementDispute d " +
            "JOIN FETCH d.settlement s " +
            "WHERE d.submittedAt >= :startDate AND d.submittedAt <= :endDate " +
            "ORDER BY d.submittedAt DESC")
    Page<SettlementDispute> findBySubmittedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    /**
     * 특정 상태의 이의신청 목록 조회
     */
    @Query("SELECT d FROM SettlementDispute d " +
            "JOIN FETCH d.settlement s " +
            "WHERE d.status = :status " +
            "ORDER BY d.submittedAt DESC")
    Page<SettlementDispute> findByStatusWithSettlement(@Param("status") SettlementDispute.DisputeProcessStatus status, Pageable pageable);
}
