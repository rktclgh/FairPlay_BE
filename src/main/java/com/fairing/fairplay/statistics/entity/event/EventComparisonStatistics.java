package com.fairing.fairplay.statistics.entity.event;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_comparison_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventComparisonStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statsId;


    @Column(name = "event_id", nullable = false)
    private Long eventId;
    private String eventTitle;
    private Long totalUsers;
    private Long totalReservations;
    private Long totalSales;
    private Long avgTicketPrice;
    private BigDecimal cancellationRate;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}

