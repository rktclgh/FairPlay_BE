package com.fairing.fairplay.temp.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AllSalesDto {
    private String eventName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private BigDecimal totalFee;
    private BigDecimal totalRevenue;

}
