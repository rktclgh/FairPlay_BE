package com.fairing.fairplay.statistics.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventGenderPopularityDto {
    private String eventTitle;
    private Long count;

    // Getters and Setters
}
