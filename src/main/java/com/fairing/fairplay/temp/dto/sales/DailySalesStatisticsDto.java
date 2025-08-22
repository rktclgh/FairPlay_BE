package com.fairing.fairplay.temp.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesStatisticsDto {
    private LocalDate date;
    private BigDecimal reservation;
    private BigDecimal ad;
    private BigDecimal booth;
    private BigDecimal etc;
}
