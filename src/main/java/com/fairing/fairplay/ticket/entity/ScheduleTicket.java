package com.fairing.fairplay.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schedule_ticket")
public class ScheduleTicket {
    
    @EmbeddedId
    private ScheduleTicketId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("scheduleId")
    @JoinColumn(name = "schedule_id")
    private EventSchedule eventSchedule;

    @Column(name = "remaining_stock", nullable = false)
    private Integer remainingStock;

    @Column(name = "sales_start_at", nullable = false)
    private LocalDateTime salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private LocalDateTime salesEndAt;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean visible = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('EVENT', 'BOOTH')")
    private ScheduleTicketType types;

    public ScheduleTicket(Ticket ticket, EventSchedule eventSchedule) {
        this.ticket = ticket;
        this.eventSchedule = eventSchedule;
        this.id = new ScheduleTicketId(ticket.getTicketId(), eventSchedule.getScheduleId());
    }
}