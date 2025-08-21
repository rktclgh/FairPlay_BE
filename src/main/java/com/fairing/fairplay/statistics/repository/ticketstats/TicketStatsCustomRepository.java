package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.reservation.EventTicketStatistics;
import com.fairing.fairplay.statistics.dto.reservation.DailyReservationRateDto;
import com.fairing.fairplay.statistics.dto.reservation.HourlyReservationRateDto;

import java.time.LocalDate;
import java.util.List;

public interface TicketStatsCustomRepository {
    List<EventTicketStatistics> calculate(LocalDate targetDate);
    List<HourlyReservationRateDto> calculateHourlyReservationRate(Long eventId, LocalDate startDate, LocalDate endDate);
    List<DailyReservationRateDto> calculateDailyReservationRate(Long eventId, LocalDate startDate, LocalDate endDate);
}