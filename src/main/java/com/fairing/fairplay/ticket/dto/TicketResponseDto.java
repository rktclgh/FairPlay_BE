package com.fairing.fairplay.ticket.dto;

import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.entity.TicketStatusCode;
import com.fairing.fairplay.ticket.entity.TypesEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {    // 티켓 정보 생성 후 응답

    private Long ticketId;
    private String name;
    private String description;
    private TicketStatusCode ticketStatusCode;
    private Integer stock;
    private Integer price;
    private Integer maxPurchase;
    private Boolean visible;
    private Boolean deleted;
    private TypesEnum types;
    private LocalDateTime createdAt;
    private Integer version;
    private String message;

    public TicketResponseDto(Ticket ticket) {
        this.ticketId = ticket.getTicketId();
        this.name = ticket.getName();
        this.description = ticket.getDescription();
        this.price = ticket.getPrice();
        this.maxPurchase = ticket.getMaxPurchase();
        this.createdAt = ticket.getCreatedAt();
    }
}
