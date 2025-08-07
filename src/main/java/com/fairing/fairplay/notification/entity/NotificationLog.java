package com.fairing.fairplay.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_code_id", nullable = false)
    private NotificationMethodCode methodCode;

    @Column(nullable = false, length = 50)
    private String status; // SUCCESS, FAIL, PENDING 등

    @Column(nullable = false)
    private Boolean isSent; // 실제 발송 성공 true, 실패 false

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
        if (this.isSent == null) this.isSent = false;
    }
}
