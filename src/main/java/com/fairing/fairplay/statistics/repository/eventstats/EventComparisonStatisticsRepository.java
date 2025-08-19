package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventComparisonStatisticsRepository extends JpaRepository<EventComparisonStatistics, Long> , EventComparisonStatsCustomRepository{
    @Modifying
    @Query(value = """
    INSERT INTO event_comparison_statistics 
    (event_id, event_title, total_users, total_reservations, total_sales, 
     avg_ticket_price, cancellation_rate, start_date, end_date, last_updated_at) 
    VALUES (:#{#stats.eventId}, :#{#stats.eventTitle}, :#{#stats.totalUsers}, 
            :#{#stats.totalReservations}, :#{#stats.totalSales}, :#{#stats.avgTicketPrice}, 
            :#{#stats.cancellationRate}, :#{#stats.startDate}, :#{#stats.endDate}, NOW())
    ON DUPLICATE KEY UPDATE 
        event_title = VALUES(event_title),
        total_users = VALUES(total_users),
        total_reservations = VALUES(total_reservations),
        total_sales = VALUES(total_sales),
        avg_ticket_price = VALUES(avg_ticket_price),
        cancellation_rate = VALUES(cancellation_rate),
        last_updated_at = NOW()
    """, nativeQuery = true)
    void upsertEventComparisonStatistics(@Param("stats") EventComparisonStatistics stats);
}
