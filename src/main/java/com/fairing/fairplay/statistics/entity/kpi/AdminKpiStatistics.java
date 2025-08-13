package com.fairing.fairplay.statistics.entity.kpi;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "admin_kpi_statistics",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_kpi_statistics_stat_date", columnNames = "stat_date")
        },
        indexes = {
        @Index(name = "idx_admin_kpi_statistics_stat_date", columnList = "stat_date")
        }
        )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminKpiStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_kpi_id")
    private Long statsKpiId;

    @Column(name = "total_events", nullable = false)
    private Long totalEvents;

    @Column(name = "total_users", nullable = false)
    private Long totalUsers;

    @Column(name = "total_reservations", nullable = false)
    private Long totalReservations;


    @Column(name = "total_sales", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

}

