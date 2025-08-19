package com.fairing.fairplay.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTicketResponseDto {    // 회차별 각 티켓 정보

    private Long ticketId;
    private String name;
    private Integer price;
    private Integer maxPurchase;            // 1인 판매제한수량
    private Integer saleQuantity;           // 판매할 수량 (화면 표시용)
    private LocalDateTime salesStartAt;
    private LocalDateTime salesEndAt;
    private Boolean visible;

}
