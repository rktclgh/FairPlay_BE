package com.fairing.fairplay.statistics.dto.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPopularityStatisticsListDto {

    private Long popularityId;
    private Long eventId; // 단순 FK ID만 저장
    private String eventTitle;
    private Long viewCount;
    private Long reservationCount;
    private Long wishlistCount;
    private String mainCategory;
    private String subCategory;
    private Integer rank;
    private LocalDateTime calculatedAt;
}
