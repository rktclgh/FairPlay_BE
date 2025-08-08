package com.fairing.fairplay.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_target_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTargetType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_target_type_id")
    private Long paymentTargetTypeId; // PK

    @Column(name = "payment_target_code", nullable = false, unique = true, length = 20)
    private String paymentTargetCode; // RESERVATION, BOOTH, AD

    @Column(name = "payment_target_name", nullable = false, length = 50)
    private String paymentTargetName; // 예약, 부스, 광고
}