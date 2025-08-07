package com.fairing.fairplay.statistics.repository.hourlystats;

import com.fairing.fairplay.statistics.entity.hourly.EventHourlyStatistics;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HourlyStatsCustomRepository {
    List<EventHourlyStatistics> calculate(LocalDate targetDate);
    List<EventHourlyStatistics> calculateWithRevenue(LocalDate targetDate);
    List<EventHourlyStatistics> calculateWithRevenueByEventAndDateRange(Long eventId, LocalDate startDate, LocalDate endDate);
    List<EventHourlyStatistics> findByEventIdAndDateRange(Long eventId, LocalDate startDate, LocalDate endDate);
    Optional<EventHourlyStatistics> findPeakHourForDate(LocalDate targetDate);
    List<EventHourlyStatistics> findTop5PeakHours(LocalDate targetDate);
}