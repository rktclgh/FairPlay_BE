package com.fairing.fairplay.statistics.entity.sales;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_daily_sales_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "stat_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDailySalesStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_sales_id")
    private Long statsSalesId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_sales", nullable = false)
    private Long totalSales = 0L;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "paid_sales", nullable = false)
    private Long paidSales = 0L;

    @Column(name = "paid_count", nullable = false)
    private Integer paidCount = 0;

    @Column(name = "cancelled_sales", nullable = false)
    private Long cancelledSales = 0L;

    @Column(name = "cancelled_count", nullable = false)
    private Integer cancelledCount = 0;

    @Column(name = "refunded_sales", nullable = false)
    private Long refundedSales = 0L;

    @Column(name = "refunded_count", nullable = false)
    private Integer refundedCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

