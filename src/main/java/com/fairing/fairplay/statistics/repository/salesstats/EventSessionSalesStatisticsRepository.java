package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.entity.sales.EventSessionSalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSessionSalesStatisticsRepository extends JpaRepository<EventSessionSalesStatistics, Long> {
}
