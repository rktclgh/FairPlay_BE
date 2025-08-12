package com.fairing.fairplay.statistics.repository.dailystats;

import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventDailyStatisticsRepository extends JpaRepository<EventDailyStatistics, Long> {
    List<EventDailyStatistics> findByEventIdOrderByStatDate(Long eventId);

    List<EventDailyStatistics> findByEventIdAndStatDateBetweenOrderByStatDate(Long eventId, LocalDate start, LocalDate end);
    List<EventDailyStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate);
}
