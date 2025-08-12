package com.fairing.fairplay.chat.listener;

import com.fairing.fairplay.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 연결/해제 시 사용자 온라인 상태를 자동으로 관리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {
    
    private final UserPresenceService userPresenceService;
    
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
            
            // SessionAttributes 안전하게 가져오기
            Long userId = null;
            if (headers.getSessionAttributes() != null) {
                userId = (Long) headers.getSessionAttributes().get("userId");
            }
        
            if (userId != null) {
                userPresenceService.setUserOnline(userId);
                log.info("WebSocket 연결: 사용자 {} 온라인 상태 설정 (Redis)", userId);
            } else {
                // Principal에서 시도 (fallback)
                Principal principal = headers.getUser();
                if (principal != null) {
                    try {
                        String username = principal.getName();
                        if (username != null && !username.isEmpty()) {
                            userId = Long.parseLong(username);
                            userPresenceService.setUserOnline(userId);
                            log.info("WebSocket 연결: 사용자 {} 온라인 상태 설정 (Redis, fallback)", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("WebSocket 연결: 사용자 ID 파싱 실패 - {}", principal.getName());
                    }
                }
            }
            
            if (userId == null) {
                log.warn("WebSocket 연결: 사용자 ID를 찾을 수 없음");
            }
            
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
            
            // SessionAttributes 안전하게 가져오기
            Long userId = null;
            if (headers.getSessionAttributes() != null) {
                userId = (Long) headers.getSessionAttributes().get("userId");
            }
            
            if (userId != null) {
                userPresenceService.setUserOffline(userId);
                log.info("WebSocket 해제: 사용자 {} 오프라인 상태 설정 (Redis)", userId);
            } else {
                // Principal에서 시도 (fallback)
                Principal principal = headers.getUser();
                if (principal != null) {
                    try {
                        String username = principal.getName();
                        if (username != null && !username.isEmpty()) {
                            userId = Long.parseLong(username);
                            userPresenceService.setUserOffline(userId);
                            log.info("WebSocket 해제: 사용자 {} 오프라인 상태 설정 (Redis, fallback)", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("WebSocket 해제: 사용자 ID 파싱 실패 - {}", principal.getName());
                    }
                }
            }
            
            if (userId == null) {
                log.warn("WebSocket 해제: 사용자 ID를 찾을 수 없음");
            }
            
        } catch (Exception e) {
            log.error("WebSocket 해제 처리 중 오류 발생", e);
        }
    }
}