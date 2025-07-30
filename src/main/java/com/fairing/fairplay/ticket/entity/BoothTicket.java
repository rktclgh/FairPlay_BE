package com.fairing.fairplay.ticket.entity;

import com.fairing.fairplay.booth.entity.Booth;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booth_ticket")
public class BoothTicket {

    @EmbeddedId
    private BoothTicketId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("boothId")
    @JoinColumn(name = "booth_id")
    private Booth booth;

    public BoothTicket(Ticket ticket, Booth booth) {
        this.ticket = ticket;
        this.booth = booth;
        this.id = new BoothTicketId(ticket.getTicketId(), booth.getId());
    }
}
