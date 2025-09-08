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

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fairing.fairplay.core.security.CustomUserDetails;

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
    public List<NotificationResponseDto> onSubscribe(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = extractUserIdFromPrincipal(principal, headerAccessor);
            if (userId != null) {
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
    public void markNotificationAsRead(@Payload Long notificationId, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = extractUserIdFromPrincipal(principal, headerAccessor);
            if (userId != null) {
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
    public void deleteNotification(@Payload Long notificationId, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = extractUserIdFromPrincipal(principal, headerAccessor);
            if (userId != null) {
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

    /**
     * Principal과 세션 속성에서 사용자 ID 추출하는 헬퍼 메소드
     * 채팅 컨트롤러와 동일한 방식으로 처리
     */
    private Long extractUserIdFromPrincipal(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        log.debug("Principal 타입: {}, 값: {}", 
            principal != null ? principal.getClass().getSimpleName() : "NULL", 
            principal != null ? principal.getName() : "NULL");
        
        // 1. 세션 속성에서 직접 추출 (가장 확실한 방법 - 채팅과 동일)
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object userId = headerAccessor.getSessionAttributes().get("userId");
            log.debug("세션 속성에서 userId 조회: {}", userId);
            if (userId instanceof Long) {
                log.info("세션 속성에서 userId 추출 성공: {}", userId);
                return (Long) userId;
            }
        }
        
        if (principal == null) {
            log.warn("세션 속성에서 userId를 찾을 수 없고 Principal도 null");
            return null;
        }
        
        // 2. StompPrincipal인 경우 (SessionHandshakeInterceptor에서 설정한 userId 문자열)
        if ("StompPrincipal".equals(principal.getClass().getSimpleName())) {
            try {
                Long userId = Long.parseLong(principal.getName());
                log.info("StompPrincipal에서 userId 추출 성공: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.warn("StompPrincipal에서 userId 파싱 실패: {}", principal.getName());
            }
        }
        
        // 3. 일반 Principal인 경우, userId 문자열 시도
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("Principal.getName()에서 userId 추출 성공: {}", userId);
            return userId;
        } catch (NumberFormatException e) {
            log.debug("Principal.getName()을 userId로 파싱할 수 없음: {}", principal.getName());
        }
        
        // 4. SecurityContext에서 CustomUserDetails 조회 (이메일 기반 Principal인 경우)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            log.info("SecurityContext에서 userId 추출 성공: {}", userDetails.getUserId());
            return userDetails.getUserId();
        }
        
        log.warn("Principal에서 userId를 추출할 수 없음: {}", principal.getName());
        return null;
    }
}