package com.fairing.fairplay.notification.service;

import com.fairing.fairplay.notification.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE(Server-Sent Events) 기반 실시간 알림 서비스
 * HTTP-only 쿠키 기반 세션 인증 사용
 *
 * 연결 수명 동안 DB 커넥션을 붙잡지 않도록 SSE 연결/전송만 담당한다.
 * 기존 알림 조회는 NotificationService의 일반 REST API 경로에서 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {

    // userId별 SseEmitter 관리 (동시성 처리)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 타임아웃: 1시간 (밀리초)
    private static final Long TIMEOUT = 60L * 60 * 1000;

    /**
     * 새로운 SSE Emitter 생성 및 등록
     *
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // 기존 연결이 있다면 종료
        removeEmitter(userId);

        // 새로운 Emitter 생성
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 이벤트 리스너 등록
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 연결 타임아웃: userId={}", userId);
            emitters.remove(userId);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            log.error("SSE 연결 에러: userId={}", userId, ex);
            emitters.remove(userId);
            emitter.completeWithError(ex);
        });

        // Emitter 등록
        emitters.put(userId, emitter);

        // 연결 확인을 위한 초기 이벤트 전송 (heartbeat)
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 성공"));
            log.info("✅ SSE Emitter 생성 및 등록 완료: userId={}", userId);
        } catch (IOException e) {
            log.error("초기 연결 이벤트 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 특정 사용자의 Emitter 제거
     *
     * @param userId 사용자 ID
     */
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE Emitter 제거: userId={}", userId);
            } catch (Exception e) {
                log.error("SSE Emitter 제거 중 에러: userId={}", userId, e);
            }
        }
    }

    /**
     * 특정 사용자에게 새 알림 전송
     *
     * @param userId       사용자 ID
     * @param notification 알림 DTO
     */
    public void sendNotification(Long userId, NotificationResponseDto notification) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.debug("SSE 연결 없음, 알림 전송 스킵: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            log.info("✅ SSE 알림 전송 성공: userId={}, title={}", userId, notification.getTitle());
        } catch (IOException e) {
            log.error("SSE 알림 전송 실패: userId={}", userId, e);
            removeEmitter(userId);
        }
    }

    /**
     * 브로드캐스트 알림 (모든 연결된 사용자에게 전송)
     *
     * @param notification 알림 DTO
     */
    public void sendBroadcast(NotificationResponseDto notification) {
        log.info("📢 브로드캐스트 알림 전송 시작: title={}, 연결된 사용자 수={}", notification.getTitle(), emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("broadcast")
                        .data(notification));
                log.debug("브로드캐스트 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("브로드캐스트 전송 실패: userId={}", userId, e);
                removeEmitter(userId);
            }
        });

        log.info("📢 브로드캐스트 알림 전송 완료");
    }

    /**
     * 알림 읽음 처리 이벤트 전송
     *
     * @param userId         사용자 ID
     * @param notificationId 알림 ID
     */
    public void sendReadConfirmation(Long userId, Long notificationId) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.debug("SSE 연결 없음, 읽음 확인 전송 스킵: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification-read")
                    .data(notificationId));
            log.info("✅ 알림 읽음 확인 전송: userId={}, notificationId={}", userId, notificationId);
        } catch (IOException e) {
            log.error("알림 읽음 확인 전송 실패: userId={}", userId, e);
            removeEmitter(userId);
        }
    }

    /**
     * 알림 삭제 이벤트 전송
     *
     * @param userId         사용자 ID
     * @param notificationId 알림 ID
     */
    public void sendDeleteConfirmation(Long userId, Long notificationId) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.debug("SSE 연결 없음, 삭제 확인 전송 스킵: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification-deleted")
                    .data(notificationId));
            log.info("✅ 알림 삭제 확인 전송: userId={}, notificationId={}", userId, notificationId);
        } catch (IOException e) {
            log.error("알림 삭제 확인 전송 실패: userId={}", userId, e);
            removeEmitter(userId);
        }
    }

    /**
     * 현재 연결된 사용자 수 조회
     *
     * @return 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }

}
