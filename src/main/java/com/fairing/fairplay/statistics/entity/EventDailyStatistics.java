package com.fairing.fairplay.statistics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_daily_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "stat_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long statsId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "reservation_count")
    private Integer reservationCount;

    @Column(name = "checkins_count")
    private Integer checkinsCount;

    @Column(name = "cancellation_count")
    private Integer cancellationCount;

    @Column(name = "no_shows_count")
    private Integer noShowsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}