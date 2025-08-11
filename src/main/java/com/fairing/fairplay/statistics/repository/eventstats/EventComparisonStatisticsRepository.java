package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventComparisonStatisticsRepository extends JpaRepository<EventComparisonStatistics, Long> , EventComparisonStatsCustomRepository{
}
