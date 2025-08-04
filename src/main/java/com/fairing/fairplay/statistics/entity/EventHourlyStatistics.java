package com.fairing.fairplay.statistics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_hourly_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "stat_date", "hour"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventHourlyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_hourly_id")
    private Long statsHourlyId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "hour", nullable = false)
    private Integer hour;

    @Column(name = "reservations")
    private Integer reservations;

    @Column(name = "checkins")
    private Integer checkins;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}