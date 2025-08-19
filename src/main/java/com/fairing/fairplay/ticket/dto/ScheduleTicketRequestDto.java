package com.fairing.fairplay.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTicketRequestDto { // 회차별 티켓 등록

    private Long ticketId;                  // 등록할 티켓 ID
    private Integer saleQuantity;           // 판매할 수량 (관리자 설정)
    private LocalDateTime salesStartAt;     // 해당 회차의 해당 티켓 판매 시작 시간
    private LocalDateTime salesEndAt;       // 해당 회차의 해당 티켓 판매 종료 시간
    private Boolean visible = false;                // 일반 사용자에 공개 여부

}
