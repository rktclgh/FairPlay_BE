package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import com.fairing.fairplay.settlement.dto.SettlementAggregationDto;
import com.fairing.fairplay.settlement.dto.SettlementAggregationRevenueDto;
import com.fairing.fairplay.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SettlementCustomRepository {
    SettlementAggregationDto aggregatedCalculate(Long eventId);

    List<SettlementAggregationRevenueDto> aggregatedRevenueCalculate(Long eventId);

    Page<Settlement> findAllByAdminUserId(Long userId, Pageable pageable);

    Page<EventManagerSettlementListDto> searchSettlement(LocalDate startDate, LocalDate endDate, String keyword, String settlementStatus, String disputeStatus, Pageable pageable);

    Page<EventManagerSettlementListDto> getAllApproveSettlement(Pageable pageable);
}
