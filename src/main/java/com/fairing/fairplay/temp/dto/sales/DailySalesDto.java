package com.fairing.fairplay.temp.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesDto {
    private LocalDate date;
    private BigDecimal reservationAmount; // 예약 매출
    private BigDecimal boothAmount; // 부스 매출
    private BigDecimal adAmount; // 광고 매출
    private BigDecimal boothApplication; // 부스 신청 매출
    private BigDecimal bannerApplication; // 배너 신청 매출
    private BigDecimal totalAmount; // 총 매출
    private Long totalCount; // 총 결제 건수
}