package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventDailySalesStatisticsRepository extends JpaRepository<EventDailySalesStatistics, Long> {
    List<EventDailySalesStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate);
}
