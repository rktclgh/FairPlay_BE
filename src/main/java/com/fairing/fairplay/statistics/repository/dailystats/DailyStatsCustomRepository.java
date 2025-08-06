package com.fairing.fairplay.statistics.repository.dailystats;


import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import java.time.LocalDate;
import java.util.List;

public interface DailyStatsCustomRepository {
    List<EventDailyStatistics> calculate(LocalDate targetDate);
}
