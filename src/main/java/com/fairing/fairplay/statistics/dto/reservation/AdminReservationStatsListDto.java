package com.fairing.fairplay.statistics.dto.reservation;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AdminReservationStatsListDto {
    private Long eventId;
    private String eventTitle;

    @Builder.Default
    private Long totalSales = 0L;

    @Builder.Default
    private Long reservationCount = 0L;

    private String mainCategory;
    private String subCategory;
    @Builder.Default
    private Integer rank = 0;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime calculatedAt;
}
