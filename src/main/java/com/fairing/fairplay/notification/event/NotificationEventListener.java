package com.fairing.fairplay.notification.event;

import com.fairing.fairplay.core.email.service.NotificationEmailService;
import com.fairing.fairplay.notification.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 리스너
 * 트랜잭션 커밋 후 SSE/이메일 전송을 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSseService sseService;
    private final NotificationEmailService notificationEmailService;

    /**
     * 트랜잭션 커밋 후 알림 전송 처리
     * SSE 전송 또는 이메일 발송
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        try {
            if ("EMAIL".equalsIgnoreCase(event.getMethodCode())) {
                // 이메일 발송
                notificationEmailService.sendEmailNotification(
                        event.getNotification().getNotificationId(),
                        event.getUserEmail(),
                        event.getNotification().getTitle(),
                        event.getNotification().getMessage(),
                        event.getNotification().getUrl()
                );
                log.info("✅ 이메일 알림 전송 완료: userId={}, email={}", event.getUserId(), event.getUserEmail());
            } else if ("WEB".equalsIgnoreCase(event.getMethodCode())) {
                // SSE로 실시간 알림 전송 (HTTP-only 쿠키 기반)
                sseService.sendNotification(event.getUserId(), event.getNotification());
                log.info("✅ SSE 알림 전송 완료: userId={}, notificationId={}",
                        event.getUserId(), event.getNotification().getNotificationId());
            }
        } catch (Exception e) {
            // SSE/이메일 전송 실패 시에도 알림은 이미 DB에 저장되어 있음
            log.error("❌ 알림 전송 실패 (DB 저장은 완료됨): userId={}, methodCode={}, error={}",
                    event.getUserId(), event.getMethodCode(), e.getMessage(), e);
        }
    }
}
