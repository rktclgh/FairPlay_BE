package com.fairing.fairplay.statistics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_session_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "session_id", "stat_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSessionStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_session_id")
    private Long statsSessionId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "ticket_type", nullable = false)
    private String ticketType;

    @Column(name = "reservations")
    private Integer reservations;

    @Column(name = "cancellation")
    private Integer cancellations;

    @Column(name = "checkins")
    private Integer checkins;

    @Column(name = "no_shows")
    private Integer noShows;


    @Column(name = "created_at")
    private LocalDateTime createdAt;
}