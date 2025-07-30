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
public class ScheduleTicketId implements Serializable {

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "schedule_id")
    private Long scheduleId;
}
