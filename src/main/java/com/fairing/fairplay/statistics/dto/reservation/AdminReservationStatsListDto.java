package com.fairing.fairplay.statistics.dto.reservation;

import com.fairing.fairplay.event.entity.MainCategory;
import com.fairing.fairplay.event.entity.SubCategory;
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

    // QueryDSL Projections용 생성자 - 파라미터 순서: eventId, eventTitle, reservationCount,
    // totalSales, mainCategory, subCategory, rank, calculatedAt
    public AdminReservationStatsListDto(Long eventId, String eventTitle, Long reservationCount, Long totalSales,
            MainCategory mainCategory, SubCategory subCategory, Integer rank, LocalDateTime calculatedAt) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.reservationCount = reservationCount != null ? reservationCount : 0L;
        this.totalSales = totalSales != null ? totalSales : 0L;
        this.mainCategory = mainCategory != null ? mainCategory.getGroupName() : null;
        this.subCategory = subCategory != null ? subCategory.getCategoryName() : null;
        this.rank = rank != null ? rank : 0;
        this.calculatedAt = calculatedAt;
    }
}
