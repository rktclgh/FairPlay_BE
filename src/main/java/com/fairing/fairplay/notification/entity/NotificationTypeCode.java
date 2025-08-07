package com.fairing.fairplay.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_type_code")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTypeCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_code_id")
    private Integer typeCodeId;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 예: RESERVATION, PAYMENT, QR 알림...

    @Column(nullable = false, length = 100)
    private String name; // 예: '예약완료', '결제완료'
}
