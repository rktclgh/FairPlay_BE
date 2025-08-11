package com.fairing.fairplay.statistics.repository.eventstats;


import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import org.springframework.data.jpa.repository.JpaRepository;



public interface EventPopularityStatisticsRepository extends JpaRepository<EventPopularityStatistics, Long> , EventPopularityStatsCustomRepository{
}
