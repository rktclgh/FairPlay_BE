package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
import java.util.List;
import java.util.Map;

// 행사별 비교 페이지 전체 응답
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventComparisonPageResponseDto {
    private List<EventComparisonResponseDto> allEvents;
    private EventStatsOverviewResponseDto overallStats;
    private List<EventComparisonResponseDto> topRevenueEvents;
    private Map<String, Long> statusCounts;
    //private List<Map<String, Object>> chartData;
}
