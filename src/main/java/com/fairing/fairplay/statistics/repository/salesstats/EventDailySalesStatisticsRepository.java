package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDailySalesStatisticsRepository extends JpaRepository<EventDailySalesStatistics, Long> {
}
