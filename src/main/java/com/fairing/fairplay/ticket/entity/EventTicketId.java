package com.fairing.fairplay.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventTicketId implements Serializable {

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "event_id")
    private Long eventId;
}
