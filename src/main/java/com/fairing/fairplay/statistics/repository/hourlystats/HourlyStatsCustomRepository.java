package com.fairing.fairplay.statistics.repository.hourlystats;


import com.fairing.fairplay.statistics.entity.EventHourlyStatistics;
import java.time.LocalDate;
import java.util.List;

public interface HourlyStatsCustomRepository {
    List<EventHourlyStatistics> calculate(LocalDate targetDate);
}