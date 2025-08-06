package com.fairing.fairplay.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fairing.fairplay.ticket.entity.TypesEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequestDto { // 티켓 정보 등록 요청

    private Long eventId;
    private Long ticketId;
    private String name;
    private String description;
    private Integer stock;
    private Integer price;
    private Integer maxPurchase;
    private TypesEnum types;    // BOOTH, EVENT

}
