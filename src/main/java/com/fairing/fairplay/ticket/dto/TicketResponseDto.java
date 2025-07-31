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
public class TicketResponseDto {    // 티켓 정보 생성 후 응답

    private String message;
    private Long ticketId;
    private LocalDateTime createdAt;
    private Integer version;
    
}
