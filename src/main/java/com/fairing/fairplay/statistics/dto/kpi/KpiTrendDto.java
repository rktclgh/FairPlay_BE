package com.fairing.fairplay.statistics.dto.kpi;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
@Setter
public class KpiTrendDto {
    LocalDate statDate;
    Long reservation;
    BigDecimal sales;
}
