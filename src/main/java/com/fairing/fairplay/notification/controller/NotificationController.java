package com.fairing.fairplay.notification.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.notification.dto.*;
import com.fairing.fairplay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    
    @Autowired
    private ApplicationContext applicationContext;

    // 내 알림 리스트
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.getUserId()));
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsReadByUser(notificationId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    // 알림 상세 조회
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDto> get(@PathVariable Long notificationId,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotificationByUser(notificationId, userDetails.getUserId()));
    }

    // 내 알림 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable Long notificationId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotificationByUser(notificationId, userDetails.getUserId());
        
        // WebSocket으로 실시간 삭제 알림 전송
        try {
            NotificationWebSocketController webSocketController = 
                    applicationContext.getBean(NotificationWebSocketController.class);
            webSocketController.sendNotificationDeleted(userDetails.getUserId(), notificationId);
        } catch (Exception e) {
            // WebSocket 전송 실패해도 삭제는 완료됨
            System.err.println("WebSocket 삭제 알림 전송 실패: " + e.getMessage());
        }
        
        return ResponseEntity.noContent().build();
    }

    // 내 알림 여러개 일괄 삭제 (옵션)
    @DeleteMapping
    public ResponseEntity<Void> deleteMultiple(@RequestParam List<Long> notificationIds,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotificationsByUser(notificationIds, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 알림 로그 목록(본인 알림만)
    @GetMapping("/{notificationId}/logs")
    public ResponseEntity<List<NotificationLogResponseDto>> getLogs(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getLogsByUser(notificationId, userDetails.getUserId()));
    }

    // 테스트용 웹소켓 알림 생성 (개발/테스트 환경에서만 사용)
    @PostMapping("/test")
    public ResponseEntity<NotificationResponseDto> createTestNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        NotificationRequestDto testDto = NotificationRequestDto.builder()
                .userId(userDetails.getUserId())
                .typeCode("SYSTEM")
                .methodCode("WEB")
                .title("테스트 웹소켓 알림")
                .message("웹소켓 알림 시스템이 정상적으로 작동합니다!")
                .url("/mypage/info")
                .build();
        
        NotificationResponseDto result = notificationService.createNotification(testDto);
        return ResponseEntity.ok(result);
    }
}
