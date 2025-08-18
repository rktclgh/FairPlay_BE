package com.fairing.fairplay.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlement_dispute")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId; // 이의신청자 ID (행사 관리자)

    @Column(name = "dispute_reason", length = 1000)
    private String disputeReason; // 이의신청 사유 (간단한 텍스트)

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SettlementDisputeFile> disputeFiles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private DisputeProcessStatus status = DisputeProcessStatus.RAISED; // SUBMITTED, UNDER_REVIEW, RESOLVED, REJECTED

    @Column(name = "admin_response", length = 2000)
    private String adminResponse; // 관리자 응답

    private LocalDateTime submittedAt; // 이의신청 접수 시간
    private LocalDateTime reviewedAt; // 검토 완료 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        submittedAt = createdAt;
        if (status == null) status = DisputeProcessStatus.RAISED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 이의신청 상태
    public enum DisputeProcessStatus {
        RAISED("이의 제기됨"),
        RESOLVED("해결완료"),
        REJECTED("반려");

        private final String description;

        DisputeProcessStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}