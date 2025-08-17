package com.fairing.fairplay.statistics.entity.sales;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_sales_statistics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_admin_sales_statistics_stat_date", columnNames = "stat_date")
        },
        indexes = {
                @Index(name = "idx_admin_sales_statistics_stat_date", columnList = "stat_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSalesStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_admin_sales_id")
    private Long statsAdminSalesId; // 통계 ID (PK)

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate; // 집계 날짜 (유니크)

    @Column(name = "total_sales", nullable = false)
    private BigDecimal totalSales; // 총 매출

    @Column(name = "reservation_revenue", nullable = false)
    private BigDecimal reservationRevenue; // 예매 수익

    @Column(name = "reservation_payment_count", nullable = false)
    private Long reservationPaymentCount;

    @Column(name = "advertising_revenue", nullable = false)
    private BigDecimal advertisingRevenue; // 광고 수익

    @Column(name = "advertising_payment_count", nullable = false)
    private Long advertisingPaymentCount;

    @Column(name = "booth_revenue", nullable = false)
    private BigDecimal boothRevenue; // 부스 수익

    @Column(name = "booth_payment_count", nullable = false)
    private Long boothPaymentCount; // 부스 결제 건수

    @Column(name = "other_revenue", nullable = false)
    private BigDecimal otherRevenue; // 기타 수익

    @Column(name = "other_payment_count", nullable = false)
    private Long otherPaymentCount;

    @Column(name = "payment_count", nullable = false)
    private Long paymentCount; // 결제 건수

    @Column(name = "average_payment_amount", nullable = false)
    private BigDecimal averagePaymentAmount; // 평균 결제 금액

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일



}
