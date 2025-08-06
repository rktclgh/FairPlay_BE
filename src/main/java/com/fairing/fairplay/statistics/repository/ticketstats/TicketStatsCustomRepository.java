package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.reservation.EventTicketStatistics;

import java.time.LocalDate;
import java.util.List;

public interface TicketStatsCustomRepository {
    List<EventTicketStatistics> calculate(LocalDate targetDate);
}