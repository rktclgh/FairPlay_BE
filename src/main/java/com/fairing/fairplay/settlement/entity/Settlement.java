package com.fairing.fairplay.settlement.entity;

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
    @Builder.Default
    private List<SettlementRevenueDetail> revenueDetails = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private TransferStatus transStatus = TransferStatus.PENDING; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private SettlementRequestStatus settlementRequestStatus = SettlementRequestStatus.PENDING; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private AdminApprovalStatus adminApprovalStatus = AdminApprovalStatus.PENDING; // PENDING, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private DisputeStatus disputeStatus = DisputeStatus.NONE; // PENDING, COMPLETED

    private LocalDate scheduledDate; // 송금 예정일
    private LocalDate completedDate; // 송금 완료일

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (transStatus == null)           transStatus = TransferStatus.PENDING;
        if (settlementRequestStatus == null) settlementRequestStatus = SettlementRequestStatus.PENDING;
        if (adminApprovalStatus == null)    adminApprovalStatus = AdminApprovalStatus.PENDING;
        if (disputeStatus == null)          disputeStatus = DisputeStatus.NONE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

