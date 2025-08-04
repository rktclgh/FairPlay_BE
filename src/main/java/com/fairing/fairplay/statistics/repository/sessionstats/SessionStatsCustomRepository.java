package com.fairing.fairplay.statistics.repository.sessionstats;

import com.fairing.fairplay.statistics.entity.EventSessionStatistics;
import java.time.LocalDate;
import java.util.List;

public interface SessionStatsCustomRepository {
    List<EventSessionStatistics> calculate(LocalDate targetDate);
}