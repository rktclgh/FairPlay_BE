package com.fairing.fairplay.settlement.dto;


import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class SettlementAggregationDto {

    private BigDecimal totalAmount; // 총 수익
    private BigDecimal feeAmount; // 수수료 차감액
    private BigDecimal finalAmount; // 최종 송금 금액
    private String revenueType; // 수익 항목 (예매, 광고, VIP 등)
    private BigDecimal revenueTypeAmount; // 금액
}
