package com.fairing.fairplay.statistics.dto.event;

import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopEventsResponseDto {
    private List<EventPopularityStatistics> topByViews;
    private List<EventPopularityStatistics> topByReservations;
    private List<EventPopularityStatistics> topByWishlists;
}
