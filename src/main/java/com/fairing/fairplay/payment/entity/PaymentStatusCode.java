package com.fairing.fairplay.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_status_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_status_code_id")
    private Integer paymentStatusCodeId;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;
}