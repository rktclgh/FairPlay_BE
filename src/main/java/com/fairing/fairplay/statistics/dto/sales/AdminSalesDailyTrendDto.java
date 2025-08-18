package com.fairing.fairplay.statistics.dto.sales;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class AdminSalesDailyTrendDto {
    private LocalDate statDate;
    private BigDecimal totalSales;
    private Long paymentCount;
}
