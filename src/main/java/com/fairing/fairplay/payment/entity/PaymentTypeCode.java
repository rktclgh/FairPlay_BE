package com.fairing.fairplay.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_type_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTypeCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_type_code_id")
    private Integer paymentTypeCodeId; // PK

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code; // 예: CARD, ACCOUNT, POINT

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 예: 카드, 계좌이체, 포인트
}
