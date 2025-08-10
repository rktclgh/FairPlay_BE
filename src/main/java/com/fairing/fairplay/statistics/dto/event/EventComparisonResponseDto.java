package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventComparisonResponseDto {
    private Long statsId;
    private Long eventId;
    private String eventName;
    private String status;
    private Long totalUsers;
    private Long totalReservations;
    private Long totalSales;
    private Long avgTicketPrice;
    private BigDecimal cancellationRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime lastUpdatedAt;
}