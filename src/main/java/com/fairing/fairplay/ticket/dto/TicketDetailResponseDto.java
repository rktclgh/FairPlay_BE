package com.fairing.fairplay.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import com.fairing.fairplay.ticket.entity.TypesEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponseDto {  // 티켓 상세 정보 응답
    private String name;
    private String description;
    private Integer stock;
    private Integer price;
    private Integer maxPurchase;
    private Integer version;
    private Integer ticketStatusCodeId;
    private boolean visible;
    private boolean deleted;
    private TypesEnum types;
    private LocalDateTime createdAt;
}
