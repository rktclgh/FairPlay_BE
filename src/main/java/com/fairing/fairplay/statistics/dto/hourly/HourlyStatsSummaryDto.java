package com.fairing.fairplay.statistics.dto.hourly;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyStatsSummaryDto {
    private Long totalReservations;
    private BigDecimal totalRevenue;
    private Double averageHourlyReservations;
    private Integer mostActiveHour;
    private String mostActiveHourDescription;
}