package com.fairing.fairplay.temp.dto.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTicketPriceDto {
    private Long eventId;
    private String eventTitle;
    private BigDecimal averageTicketPrice; // 평균 티켓 가격
}
