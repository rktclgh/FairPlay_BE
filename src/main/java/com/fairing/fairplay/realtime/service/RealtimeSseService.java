package com.fairing.fairplay.realtime.service;

import com.fairing.fairplay.booth.dto.WaitingMessage;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeSseService {

    private static final long TIMEOUT = 60L * 60L * 1000L;
    private static final long HEARTBEAT_DELAY = 25_000L;

    private final QrTicketRepository qrTicketRepository;
    private final ConcurrentHashMap<Long, Set<SseEmitter>> qrTicketEmitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<SseEmitter>> waitingEmitters = new ConcurrentHashMap<>();

    public SseEmitter openQrTicketStream(Long qrTicketId, Long userId) {
        if (!qrTicketRepository.existsById(qrTicketId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "QR 티켓을 찾을 수 없습니다.");
        }
        if (!qrTicketRepository.existsByIdAndReservationUserId(qrTicketId, userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "QR 티켓 실시간 상태를 구독할 권한이 없습니다.");
        }
        return register(qrTicketEmitters, qrTicketId, "qr-ticket-connected", "QR 티켓 실시간 연결 성공");
    }

    public SseEmitter openWaitingStream(Long userId) {
        return register(waitingEmitters, userId, "waiting-connected", "웨이팅 실시간 연결 성공");
    }

    public void sendQrTicketEvent(Long qrTicketId, String message) {
        if (qrTicketId == null) {
            return;
        }
        send(qrTicketEmitters, qrTicketId, "qr-ticket-status", message);
    }

    public void sendWaitingEvent(Long userId, WaitingMessage message) {
        if (userId == null) {
            return;
        }
        send(waitingEmitters, userId, "waiting-status", message);
    }

    @Scheduled(fixedDelay = HEARTBEAT_DELAY)
    public void sendHeartbeat() {
        sendHeartbeat(qrTicketEmitters);
        sendHeartbeat(waitingEmitters);
    }

    private SseEmitter register(ConcurrentHashMap<Long, Set<SseEmitter>> registry, Long key,
                                String eventName, String message) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        registry.compute(key, (ignored, emitters) -> {
            Set<SseEmitter> activeEmitters = emitters;
            if (activeEmitters == null) {
                activeEmitters = ConcurrentHashMap.newKeySet();
            }
            activeEmitters.add(emitter);
            return activeEmitters;
        });

        Runnable cleanup = () -> remove(registry, key, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(error -> {
            cleanup.run();
            log.debug("SSE stream closed with error. key={}, event={}", key, eventName, error);
        });

        try {
            sendEmitter(emitter, SseEmitter.event().name(eventName).data(message));
        } catch (IOException e) {
            cleanup.run();
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void send(ConcurrentHashMap<Long, Set<SseEmitter>> registry, Long key, String eventName,
                      Object payload) {
        Set<SseEmitter> emitters = registry.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                sendEmitter(emitter, SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                remove(registry, key, emitter);
                emitter.completeWithError(e);
            }
        }
    }

    private void sendHeartbeat(ConcurrentHashMap<Long, Set<SseEmitter>> registry) {
        registry.forEach((key, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    sendEmitter(emitter, SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException e) {
                    remove(registry, key, emitter);
                    emitter.completeWithError(e);
                }
            }
        });
    }

    private void sendEmitter(SseEmitter emitter, SseEmitter.SseEventBuilder event)
            throws IOException {
        synchronized (emitter) {
            emitter.send(event);
        }
    }

    private void remove(ConcurrentHashMap<Long, Set<SseEmitter>> registry, Long key, SseEmitter emitter) {
        registry.computeIfPresent(key, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
