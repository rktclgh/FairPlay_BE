package com.fairing.fairplay.notification.event;

import com.fairing.fairplay.notification.dto.NotificationResponseDto;
import lombok.Getter;

/**
 * 알림 생성 이벤트
 * 트랜잭션 커밋 후 SSE 전송을 위한 이벤트
 */
@Getter
public class NotificationCreatedEvent {
    private final Long userId;
    private final String methodCode;
    private final NotificationResponseDto notification;
    private final String userEmail; // 이메일 전송용

    public NotificationCreatedEvent(Long userId, String methodCode, NotificationResponseDto notification, String userEmail) {
        this.userId = userId;
        this.methodCode = methodCode;
        this.notification = notification;
        this.userEmail = userEmail;
    }
}
