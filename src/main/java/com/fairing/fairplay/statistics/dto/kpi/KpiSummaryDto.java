package com.fairing.fairplay.statistics.dto.kpi;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
public class KpiSummaryDto {
    private Long totalEvents;
    private Long totalUsers;
    private Long totalReservations;
    private BigDecimal totalSales;
}
