package com.fairing.fairplay.temp.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesByTargetTypeDto {
    private LocalDate date;
    private String paymentTargetType; // 결제 대상 타입 (예: TICKET, BOOTH 등)
    private BigDecimal totalAmount;
    private Long transactionCount;
}
