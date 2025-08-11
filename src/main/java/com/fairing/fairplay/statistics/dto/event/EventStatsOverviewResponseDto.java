package com.fairing.fairplay.statistics.dto.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatsOverviewResponseDto {
    private Long totalUsers;
    private Long totalReservations;
    private Long totalSales;
    private Long totalEvents;
}
