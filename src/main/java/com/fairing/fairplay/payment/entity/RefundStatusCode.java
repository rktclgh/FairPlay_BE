package com.fairing.fairplay.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refund_status_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundStatusCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_status_code_id")
    private Integer refundStatusCodeId; // PK

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code; // REQUESTED, APPROVED, REJECTED

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 요청, 승인, 거부
}

