package com.fairing.fairplay.temp.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCompareDto {
    private String eventName;
    private int status;
    private Long userCount;
    private Long reservationCount;
    private BigDecimal totalRevenue;
    private Long averageTicketPrice;
    private double cancelRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate modifyDate;
}
