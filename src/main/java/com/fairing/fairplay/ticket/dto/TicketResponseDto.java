package com.fairing.fairplay.ticket.dto;

import com.fairing.fairplay.ticket.entity.*;
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
    private String audienceTypeCode;
    private String audienceTypeName;
    private String seatTypeCode;
    private String seatTypeName;
    private String ticketStatusCode;
    private String ticketStatusName;
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
        
        // Entity 관계 필드들은 null 체크 후 안전하게 접근
        if (ticket.getTicketStatusCode() != null) {
            this.ticketStatusCode = ticket.getTicketStatusCode().getCode();
            this.ticketStatusName = ticket.getTicketStatusCode().getName();
        }
        if (ticket.getTicketAudienceType() != null) {
            this.audienceTypeCode = ticket.getTicketAudienceType().getCode();
            this.audienceTypeName = ticket.getTicketAudienceType().getName();
        }
        if (ticket.getTicketSeatType() != null) {
            this.seatTypeCode = ticket.getTicketSeatType().getCode();
            this.seatTypeName = ticket.getTicketSeatType().getName();
        }
    }
}
