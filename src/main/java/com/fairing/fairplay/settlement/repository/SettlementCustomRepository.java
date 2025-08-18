package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.dto.SettlementAggregationDto;
import com.fairing.fairplay.settlement.dto.SettlementAggregationRevenueDto;
import com.fairing.fairplay.settlement.entitiy.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SettlementCustomRepository {
    SettlementAggregationDto aggregatedCalculate(Long eventId);

    List<SettlementAggregationRevenueDto> aggregatedRevenueCalculate(Long eventId);

    Page<Settlement> findAllByAdminUserId(Long userId, Pageable pageable);
}
