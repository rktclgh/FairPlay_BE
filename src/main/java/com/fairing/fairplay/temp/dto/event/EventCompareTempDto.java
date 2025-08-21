package com.fairing.fairplay.temp.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCompareTempDto {
    private Long eventId;
    private Integer status;
    private String eventName;
    private Long userCount;
    private Long reservationCount;
    private BigDecimal totalRevenue;
    private Long cancelCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime updatedAt;
}
