package com.fairing.fairplay.statistics.dto.reservation;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReservationSummaryDto {
    private Integer totalReservations;
    private BigDecimal totalSales;
    private Integer totalCancellations;
    private BigDecimal avgSales;
    private List<AdminReservationStatsByCategoryDto> categoryTrend;
}
