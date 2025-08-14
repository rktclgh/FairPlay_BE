package com.fairing.fairplay.statistics.dto.event;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatsOverviewResponseDto {
    private Long totalUsers;
    private Long totalReservations;
    private BigDecimal totalSales;
    private Long totalEvents;
}
