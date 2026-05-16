package com.fairing.fairplay.statistics.repository.eventstats;


import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface EventPopularityStatisticsRepository extends JpaRepository<EventPopularityStatistics, Long> , EventPopularityStatsCustomRepository{
    @Modifying
    @Query(value = """
    INSERT INTO event_popularity_statistics 
    (event_id, event_title, view_count, reservation_count, wishlist_count, calculated_at) 
    VALUES (:#{#stats.eventId}, :#{#stats.eventTitle}, :#{#stats.viewCount}, 
            :#{#stats.reservationCount}, :#{#stats.wishlistCount}, :#{#stats.calculatedAt})
    ON CONFLICT (event_id) DO UPDATE SET
        event_title = EXCLUDED.event_title,
        view_count = EXCLUDED.view_count,
        reservation_count = EXCLUDED.reservation_count,
        wishlist_count = EXCLUDED.wishlist_count,
        calculated_at = EXCLUDED.calculated_at
    """, nativeQuery = true)
    void upsertEventPopularityStatistics(@Param("stats") EventPopularityStatistics stats);
}
