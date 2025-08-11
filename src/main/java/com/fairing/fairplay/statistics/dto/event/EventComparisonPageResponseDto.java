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
    @Builder.Default
    private List<EventComparisonResponseDto> allEvents = java.util.Collections.emptyList();
    private EventStatsOverviewResponseDto overallStats;
    @Builder.Default
    private List<EventComparisonResponseDto> topRevenueEvents = java.util.Collections.emptyList();
    @Builder.Default
    private Map<String, Long> statusCounts = java.util.Collections.emptyMap();
}
