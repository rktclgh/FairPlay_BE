package com.fairing.fairplay.payment.entity;

import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount; // 환불 금액

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_status_code_id", nullable = false)
    private RefundStatusCode refundStatusCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Users approvedBy; // 승인한 관리자

    @Column(name = "admin_comment", length = 1000)
    private String adminComment; // 관리자 코멘트

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // PG사 환불 처리 완료 시간

    @Column(name = "failure_reason", length = 500)
    private String failureReason; // 환불 실패 사유
}
