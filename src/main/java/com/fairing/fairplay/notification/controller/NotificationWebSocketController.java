package com.fairing.fairplay.notification.controller;

import com.fairing.fairplay.notification.dto.NotificationResponseDto;
import com.fairing.fairplay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    /**
     * 특정 사용자에게 실시간 알림 전송
     */
    public void sendNotificationToUser(Long userId, NotificationResponseDto notification) {
        try {
            log.info("실시간 알림 전송: userId={}, title={}", userId, notification.getTitle());
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/topic/notifications",
                notification
            );
        } catch (Exception e) {
            log.error("실시간 알림 전송 실패: userId={}", userId, e);
        }
    }

    /**
     * 알림 구독 시 기존 알림 목록 전송
     */
    @SubscribeMapping("/topic/notifications")
    public List<NotificationResponseDto> onSubscribe(Principal principal) {
        try {
            if (principal != null) {
                Long userId = Long.parseLong(principal.getName());
                log.info("알림 구독: userId={}", userId);
                return notificationService.getNotifications(userId);
            }
        } catch (Exception e) {
            log.error("알림 구독 처리 실패", e);
        }
        return List.of();
    }

    /**
     * 알림 읽음 처리 WebSocket 메시지 핸들링
     */
    @MessageMapping("/notifications/markRead")
    public void markNotificationAsRead(@Payload Long notificationId, Principal principal) {
        try {
            if (principal != null) {
                Long userId = Long.parseLong(principal.getName());
                notificationService.markAsReadByUser(notificationId, userId);
                log.info("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
                
                // 읽음 처리 완료를 해당 사용자에게 알림
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/topic/notifications/read",
                    notificationId
                );
            }
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}", notificationId, e);
        }
    }

    /**
     * 알림 삭제 WebSocket 메시지 핸들링
     */
    @MessageMapping("/notifications/delete")
    public void deleteNotification(@Payload Long notificationId, Principal principal) {
        try {
            if (principal != null) {
                Long userId = Long.parseLong(principal.getName());
                notificationService.deleteNotificationByUser(notificationId, userId);
                log.info("알림 삭제 처리: notificationId={}, userId={}", notificationId, userId);
                
                // 삭제 완료를 해당 사용자에게 알림
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/topic/notifications/deleted",
                    notificationId
                );
            }
        } catch (Exception e) {
            log.error("알림 삭제 처리 실패: notificationId={}", notificationId, e);
        }
    }

    /**
     * 전체 사용자에게 공지사항 형태의 알림 전송
     */
    public void sendBroadcastNotification(NotificationResponseDto notification) {
        try {
            log.info("브로드캐스트 알림 전송: title={}", notification.getTitle());
            messagingTemplate.convertAndSend("/topic/notifications/broadcast", notification);
        } catch (Exception e) {
            log.error("브로드캐스트 알림 전송 실패", e);
        }
    }

    /**
     * 특정 사용자에게 알림 삭제 완료 알림 전송
     */
    public void sendNotificationDeleted(Long userId, Long notificationId) {
        try {
            log.info("알림 삭제 완료 알림 전송: userId={}, notificationId={}", userId, notificationId);
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/topic/notifications/deleted",
                notificationId
            );
        } catch (Exception e) {
            log.error("알림 삭제 완료 알림 전송 실패: userId={}, notificationId={}", userId, notificationId, e);
        }
    }
}