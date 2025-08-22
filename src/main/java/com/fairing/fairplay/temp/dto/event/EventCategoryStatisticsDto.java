package com.fairing.fairplay.temp.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventCategoryStatisticsDto {

    private String categoryName;
    private Long totalViewCount;
    private Long totalEventCount;
    private Long totalWishlistCount;

}
