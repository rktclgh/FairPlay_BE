package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import java.time.LocalDate;
import java.util.List;

public interface EventComparisonStatsCustomRepository {
    List<EventComparisonStatistics> calculate(LocalDate targetDate);
    List<EventComparisonStatistics> findByStatus(String status, LocalDate currentDate);
}
