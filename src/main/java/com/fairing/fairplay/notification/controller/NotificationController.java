package com.fairing.fairplay.notification.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.notification.dto.*;
import com.fairing.fairplay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 생성
    @PostMapping
    public ResponseEntity<NotificationResponseDto> create(@RequestBody NotificationRequestDto dto) {
        return ResponseEntity.ok(notificationService.createNotification(dto));
    }

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
    public ResponseEntity<List<?>> getLogs(@PathVariable Long notificationId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getLogsByUser(notificationId, userDetails.getUserId()));
    }
}
