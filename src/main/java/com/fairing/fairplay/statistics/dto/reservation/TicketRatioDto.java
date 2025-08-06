package com.fairing.fairplay.statistics.dto.reservation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TicketRatioDto {
    private String ticketType;
    private Integer reservations;
}
