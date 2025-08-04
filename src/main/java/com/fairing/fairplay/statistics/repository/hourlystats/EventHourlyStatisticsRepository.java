package com.fairing.fairplay.statistics.repository.hourlystats;

import com.fairing.fairplay.statistics.entity.EventHourlyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHourlyStatisticsRepository extends JpaRepository<EventHourlyStatistics, Long>,HourlyStatsCustomRepository {}
