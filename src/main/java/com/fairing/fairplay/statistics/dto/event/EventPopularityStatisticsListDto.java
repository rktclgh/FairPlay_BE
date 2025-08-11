package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @Builder.Default
    private Long viewCount = 0L;
    @Builder.Default
    private Long reservationCount = 0L;
    @Builder.Default
    private Long wishlistCount = 0L;
    private String mainCategory;
    private String subCategory;
    @Builder.Default
    private Integer rank = 0;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime calculatedAt;
}
