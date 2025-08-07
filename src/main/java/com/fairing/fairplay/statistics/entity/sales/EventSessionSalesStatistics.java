package com.fairing.fairplay.statistics.entity.sales;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_session_sales_statistics")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class EventSessionSalesStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_sales_session_id")
    private Long statsSalesSessionId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "ticket_name", nullable = false)
    private String ticketName;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "payment_status_code", nullable = false)
    private String paymentStatusCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
