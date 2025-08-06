package com.fairing.fairplay.statistics.repository.sessionstats;

import com.fairing.fairplay.statistics.entity.reservation.EventSessionStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventSessionStatisticsRepository extends JpaRepository<EventSessionStatistics, Long>,SessionStatsCustomRepository {
    List<EventSessionStatistics> findByEventIdAndStatDateBetween(Long eventId, LocalDate start, LocalDate end);
}
