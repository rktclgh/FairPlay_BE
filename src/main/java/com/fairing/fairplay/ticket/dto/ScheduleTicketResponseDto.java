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
    private Integer remainingStock;
    private LocalDateTime salesStartAt;
    private LocalDateTime salesEndAt;
    private Boolean visible;

}
