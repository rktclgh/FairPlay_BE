package com.fairing.fairplay.statistics.dto.kpi;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.YearMonth;

@Builder
@Getter
@Setter

public class KpiTrendMonthlyDto {

    @JsonFormat(pattern = "yyyy-MM")
    private YearMonth yearMonth;

    private Long reservations;
    private BigDecimal sales;
}
