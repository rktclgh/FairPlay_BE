package com.fairing.fairplay.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Users FK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code_id", nullable = false)
    private NotificationTypeCode typeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_code_id", nullable = false)
    private NotificationMethodCode methodCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 500)
    private String url;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }

    // Soft Delete 메서드
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 삭제 여부 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
