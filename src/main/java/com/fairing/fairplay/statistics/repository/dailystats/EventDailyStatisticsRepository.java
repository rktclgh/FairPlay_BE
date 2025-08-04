package com.fairing.fairplay.statistics.repository.dailystats;

import com.fairing.fairplay.statistics.entity.EventDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventDailyStatisticsRepository extends JpaRepository<EventDailyStatistics, Long>,DailyStatsCustomRepository {
    List<EventDailyStatistics> findByEventIdOrderByStatDate(Long eventId);
}
