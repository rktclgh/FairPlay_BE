package com.fairing.fairplay.settlement.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class SettlementAggregationRevenueDto {

    private String revenueType; // 수익 항목 (예매, 광고, VIP 등)
    private BigDecimal revenueTypeAmount; // 금액
}
