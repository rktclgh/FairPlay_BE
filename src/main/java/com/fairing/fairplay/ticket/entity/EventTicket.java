package com.fairing.fairplay.ticket.entity;

import com.fairing.fairplay.event.entity.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "event_ticket")
public class EventTicket {

    @EmbeddedId
    private EventTicketId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private Event event;

    public EventTicket(Ticket ticket, Event event) {
        this.ticket = ticket;
        this.event = event;
        this.id = new EventTicketId(ticket.getTicketId(), event.getEventId());
    }
}
