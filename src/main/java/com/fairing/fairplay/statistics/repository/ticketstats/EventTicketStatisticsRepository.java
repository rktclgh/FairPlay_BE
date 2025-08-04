package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.EventTicketStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EventTicketStatisticsRepository extends JpaRepository<EventTicketStatistics, Long>, TicketStatsCustomRepository {
    List<EventTicketStatistics> findByEventIdAndStatDateBetween(Long eventId, LocalDate start, LocalDate end);
}
