package com.fairing.fairplay.statistics.dto.kpi;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Builder
@Getter
@Setter

public class KpiTrendMonthlyDto {
    private YearMonth yearMonth;
    private long reservations;
    private BigDecimal sales;
}
