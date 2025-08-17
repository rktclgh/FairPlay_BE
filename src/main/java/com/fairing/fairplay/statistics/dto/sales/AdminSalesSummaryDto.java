package com.fairing.fairplay.statistics.dto.sales;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminSalesSummaryDto {
    private BigDecimal totalSales;
    private Long paymentCount;
    private BigDecimal averageDailyPaymentAmount;

}
