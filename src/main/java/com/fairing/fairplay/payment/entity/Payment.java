package com.fairing.fairplay.payment.entity;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = true)
    private Event event; // 결제가 속한 이벤트 (광고 결제 등은 이벤트와 무관할 수 있음)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // 결제 요청자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_target_type_id", nullable = false)
    private PaymentTargetType paymentTargetType; // 결제 대상 타입 (예약/부스/광고)

    @Column(name = "target_id", nullable = false)
    private Long targetId; // 실제 결제 대상의 ID (reservation_id, booth_id, advertisement_id)

    @Column(name = "merchant_uid", nullable = false, unique = true, length = 100)
    private String merchantUid;

    @Column(name = "imp_uid", length = 100)
    private String impUid;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 구매 수량

    @Column(name = "price", nullable = false)
    private BigDecimal price; // 개당 가격

    @Builder.Default
    @Column(name = "refunded_amount", precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // 총 환불 금액

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "pg_provider", length = 50)
    private String pgProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_code_id", nullable = false)
    private PaymentTypeCode paymentTypeCode; // 별도 코드 엔티티

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_code_id", nullable = false)
    private PaymentStatusCode paymentStatusCode;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Refund> refunds;
}