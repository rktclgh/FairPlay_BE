package com.fairing.fairplay.statistics.entity.reservation;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_ticket_statistics", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "stat_date",
        "ticket_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTicketStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_ticket_id")
    private Long statsTicketId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "ticket_type", nullable = false)
    private String ticketType;

    @Column(name = "reservations")
    private Integer reservations;

    private Integer stock;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cancellations")
    @Builder.Default
    private Integer cancellations = 0;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}