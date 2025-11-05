package com.fairing.fairplay.notification.controller;

import com.fairing.fairplay.notification.dto.NotificationResponseDto;
import com.fairing.fairplay.notification.service.NotificationSseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * SSE(Server-Sent Events) 기반 알림 컨트롤러
 * HTTP-only 쿠키 기반 세션 인증 사용
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSseController {

    private final NotificationSseService sseService;

    /**
     * SSE 스트림 연결 - HTTP-only 쿠키로 자동 인증
     *
     * @param session HTTP 세션 (쿠키에서 자동 추출)
     * @return SseEmitter 스트림
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpSession session) {
        // HTTP-only 쿠키 기반 세션에서 userId 추출
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            log.debug("SSE 연결 시도했으나 세션에 userId 없음 - 비로그인 상태, 빈 emitter 반환 후 즉시 종료");
            // 🔒 비로그인 사용자: 예외를 던지지 않고 빈 emitter를 생성해서 즉시 완료 처리
            // EventSource는 자동 재연결을 시도하지 않고 조용히 종료됨
            SseEmitter emitter = new SseEmitter(1000L); // 1초 타임아웃
            try {
                emitter.complete(); // 즉시 연결 종료
            } catch (Exception e) {
                log.error("빈 SSE emitter 종료 실패:", e);
            }
            return emitter;
        }

        log.info("✅ SSE 스트림 연결: userId={}", userId);

        // SSE Emitter 생성 및 등록
        SseEmitter emitter = sseService.createEmitter(userId);

        // 연결 즉시 기존 알림 목록 전송
        sseService.sendInitialNotifications(userId, emitter);

        return emitter;
    }

    /**
     * SSE 연결 종료 (명시적 종료용)
     */
    @DeleteMapping("/stream")
    public ResponseEntity<Void> closeStream(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("SSE 스트림 명시적 종료 요청: userId={}", userId);
        sseService.removeEmitter(userId);

        return ResponseEntity.ok().build();
    }
}
