package com.fairing.fairplay.ticket.dto;

import com.fairing.fairplay.ticket.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fairing.fairplay.ticket.entity.TypesEnum;

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
    private TypesEnum types;

    public TicketSnapshotDto(Ticket ticket) {
        this.name = ticket.getName();
        this.description = ticket.getDescription();

        if(ticket.getTicketStatusCode() != null){
            this.ticketStatusCodeId = ticket.getTicketStatusCode().getTicketStatusCodeId();
        }

        this.stock = ticket.getStock();
        this.price = ticket.getPrice();
        this.maxPurchase = ticket.getMaxPurchase();
        this.visible = ticket.getVisible();
        this.deleted = ticket.getDeleted();
        this.types = ticket.getTypes();
    }

}
