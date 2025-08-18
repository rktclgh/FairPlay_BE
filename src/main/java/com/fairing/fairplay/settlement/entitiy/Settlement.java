package com.fairing.fairplay.settlement.entitiy;

import com.fairing.fairplay.event.entity.Event;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "event_title" , nullable = false)
    private String eventTitle; // 행사 ID (FK)

    @Column(name = "total_amount",nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount; // 총 수익

    @Column(name = "fee_amount",nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmount; // 수수료 차감액

    @Column(name = "final_amount",nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount; // 최종 송금 금액

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementRevenueDetail> revenueDetails = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TransferStatus transStatus; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementRequestStatus settlementRequestStatus; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AdminApprovalStatus adminApprovalStatus; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DisputeStatus disputeStatus; // PENDING, COMPLETED

    private LocalDate scheduledDate; // 송금 예정일
    private LocalDate completedDate; // 송금 완료일

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

