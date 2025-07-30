package com.fairing.fairplay.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSnapshotDto {

    private String name;
    private String description;
    private Integer ticketStatusCodeId;
    private Integer stock;
    private Integer price;
    private Integer maxPurchase;
    private boolean visible;
    private boolean deleted;

}
