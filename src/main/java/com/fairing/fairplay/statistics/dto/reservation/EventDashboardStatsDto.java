package com.fairing.fairplay.statistics.dto.reservation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;


@Getter
@Setter
@Builder
public class EventDashboardStatsDto {
    private ReservationSummaryDto summary;
    private List<ReservationDailyTrendDto> dailyTrend;
    private List<TicketRatioDto> ticketRatio;
    private List<SessionStatsDto> sessionStats;
}
