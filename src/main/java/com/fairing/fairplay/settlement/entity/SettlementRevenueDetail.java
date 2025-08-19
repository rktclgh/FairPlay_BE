package com.fairing.fairplay.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_revenue_detail")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRevenueDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement; // 정산과 N:1 관계

    @Column(nullable = false, length = 50)
    private String revenueType; // 수익 항목 (예매, 광고, VIP 등)

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // 금액

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}