package com.fairing.fairplay.payment.entity;

import com.fairing.fairplay.reservation.entity.Reservation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_code_id", nullable = false)
    private PaymentTypeCode paymentTypeCode; // 별도 코드 엔티티

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_code_id", nullable = false)
    private PaymentStatusCode paymentStatusCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}