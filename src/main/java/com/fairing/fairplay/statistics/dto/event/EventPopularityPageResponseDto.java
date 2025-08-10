package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPopularityPageResponseDto {
    private List<EventPopularityStatisticsListDto> allEvents;
    private PopularityOverviewResponseDto overview;
    private TopEventsResponseDto topEvents;

}
