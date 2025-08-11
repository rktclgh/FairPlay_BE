package com.fairing.fairplay.statistics.entity.event;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_comparison_statistics",
        indexes = {
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_start_date", columnList = "start_date"),
        @Index(name = "idx_end_date", columnList = "end_date"),
        @Index(name = "uk_event_period", columnList = "event_id,start_date,end_date", unique = true)
}
        )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventComparisonStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_comparison_id", nullable = false)
    private Long statsComparisonId;


    @Column(name = "event_id", nullable = false)
    private Long eventId;
    private String eventTitle;
    private Long totalUsers;
    private Long totalReservations;
    private Long totalSales;
    private Long avgTicketPrice;

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal cancellationRate = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    public void prePersist() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}

