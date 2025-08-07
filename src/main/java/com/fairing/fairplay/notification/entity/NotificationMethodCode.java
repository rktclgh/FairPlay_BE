package com.fairing.fairplay.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_method_code")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationMethodCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "method_code_id")
    private Integer methodCodeId;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 예: EMAIL, WEB, PUSH

    @Column(nullable = false, length = 100)
    private String name; // 예: '이메일', '웹 알림'
}
