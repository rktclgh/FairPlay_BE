package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.dto.event.TopEventsResponseDto;
import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface EventPopularityStatsCustomRepository {
    List<EventPopularityStatistics> calculate(LocalDate targetDate);

    Page<EventPopularityStatisticsListDto> paymentCountPopularity(
            LocalDate startDate,
            LocalDate endDate,
            String mainCategory,
            String subCategory,
            Pageable pageable  // 페이징 정보 추가
    );

    List<EventPopularityStatisticsListDto> aggregatedPopularity(LocalDate startDate, LocalDate endDate, String mainCategory, String subCategory );
    TopEventsResponseDto topAggregatedPopularity(LocalDate startDate, LocalDate endDate);
    List<EventPopularityStatisticsListDto> searchEventWithRank(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory);
}
