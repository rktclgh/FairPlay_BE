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
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();
        
        if (principal != null) {
            try {
                // JWT에서 사용자 ID 추출 (간단한 방법)
                String username = principal.getName();
                if (username != null && !username.isEmpty()) {
                    Long userId = Long.parseLong(username);
                    userPresenceService.setUserOnline(userId);
                    log.info("WebSocket 연결: 사용자 {} 온라인 상태 설정", userId);
                }
            } catch (NumberFormatException e) {
                log.warn("WebSocket 연결: 사용자 ID 파싱 실패 - {}", principal.getName());
            }
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();
        
        if (principal != null) {
            try {
                String username = principal.getName();
                if (username != null && !username.isEmpty()) {
                    Long userId = Long.parseLong(username);
                    userPresenceService.setUserOffline(userId);
                    log.info("WebSocket 해제: 사용자 {} 오프라인 상태 설정", userId);
                }
            } catch (NumberFormatException e) {
                log.warn("WebSocket 해제: 사용자 ID 파싱 실패 - {}", principal.getName());
            }
        }
    }
}