package com.fairing.fairplay.statistics.dto.sales;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Getter
@Setter
@Builder
public class AdminSalesMonthlyTrendDto {
    private YearMonth statDate;
    private BigDecimal totalSales;
    private Long paymentCount;
}
