package com.fairing.fairplay.statistics.service.event;

import com.fairing.fairplay.statistics.dto.event.EventPopularityPageResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.dto.event.PopularityOverviewResponseDto;
import com.fairing.fairplay.statistics.dto.event.TopEventsResponseDto;
import com.fairing.fairplay.statistics.repository.eventstats.EventPopularityStatsCustomRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPopularityService {

    private final EventPopularityStatsCustomRepositoryImpl popularityRepository;

    public EventPopularityPageResponseDto getPopularityPageData(LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수 입력 값입니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일 이전이어야 합니다.");
        }

        // 1. 전체 인기 행사 통계
       List<EventPopularityStatisticsListDto> allStats = popularityRepository.aggregatedPopularity(startDate,  endDate ,"" ,"");

        // 2. 개요 통계 계산
        PopularityOverviewResponseDto overview = calculateOverview(allStats);

        // 3. TOP 5 리스트들
        TopEventsResponseDto topEvents = popularityRepository.topAggregatedPopularity(startDate, endDate);


        return EventPopularityPageResponseDto.builder()
                .allEvents(allStats)
                .overview(overview)
                .topEvents(topEvents)
                .build();
    }

    /**
     * 검색 기능
     */
    public List<EventPopularityStatisticsListDto> searchEvents(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory) {
        if (mainCategory != null && "all".equalsIgnoreCase(mainCategory)) {
            mainCategory = "";
        }
        if (subCategory != null && "all".equalsIgnoreCase(subCategory)) {
            subCategory = "";
        }
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어(keyword)는 null 또는 빈 문자열일 수 없습니다.");
        }
        return popularityRepository.searchEventWithRank(startDate,  endDate,  keyword, mainCategory,  subCategory);
    }

    /**
     * 카테고리별 인기 행사 조회
     */
    public Page<EventPopularityStatisticsListDto> getEventsByCategory(LocalDate startDate, LocalDate endDate, String mainCategory, String subCategory, Pageable pageable) {
        if (mainCategory != null && "all".equalsIgnoreCase(mainCategory)) {
            mainCategory = "";
        }
        if (subCategory != null && "all".equalsIgnoreCase(subCategory)) {
            subCategory = "";
        }
        return popularityRepository.aggregatedPopularity(startDate, endDate, mainCategory, subCategory, pageable);
    }

    private PopularityOverviewResponseDto calculateOverview(List<EventPopularityStatisticsListDto> allStats) {
        long totalEvents = allStats.size();

        double avgViews = allStats.stream()
                .mapToLong(s -> s.getViewCount() != null ? s.getViewCount() : 0)
                .average().orElse(0.0);

        double avgReservations = allStats.stream()
                .mapToLong(s -> s.getReservationCount() != null ? s.getReservationCount() : 0)
                .average().orElse(0.0);

        double avgWishlists = allStats.stream()
                .mapToLong(s -> s.getWishlistCount() != null ? s.getWishlistCount() : 0)
                .average().orElse(0.0);

        return PopularityOverviewResponseDto.builder()
                .totalEvents(totalEvents)
                .avgViewCount(Math.round(avgViews))
                .avgReservationCount(Math.round(avgReservations))
                .avgWishlistCount(Math.round(avgWishlists))
                .build();
    }
}
