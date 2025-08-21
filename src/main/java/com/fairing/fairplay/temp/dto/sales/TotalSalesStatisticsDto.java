package com.fairing.fairplay.temp.dto.sales;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TotalSalesStatisticsDto {
    private BigDecimal totalRevenue;
    private Long totalPayments;
    private BigDecimal averageSales;

}
