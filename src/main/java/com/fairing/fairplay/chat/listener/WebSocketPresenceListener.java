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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fairing.fairplay.core.security.CustomUserDetails;

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
            
            // 1. 세션 속성에서 우선 추출 (가장 확실한 방법)
            Long userId = extractUserIdFromSessionAttributes(headers);
            log.debug("WebSocket 연결 이벤트 - SessionAttributes에서 userId 조회: {}", userId);
            if (headers.getSessionAttributes() != null) {
                log.debug("WebSocket 연결 이벤트 - 모든 SessionAttributes: {}", headers.getSessionAttributes());
            } else {
                log.debug("WebSocket 연결 이벤트 - SessionAttributes가 null");
            }
        
            if (userId != null) {
                userPresenceService.setUserOnline(userId);
                log.info("WebSocket 연결: 사용자 {} 온라인 상태 설정 (Redis)", userId);
            } else {
                // 2. Principal에서 시도 (fallback)
                Principal principal = headers.getUser();
                log.debug("WebSocket 연결 이벤트 - Principal: {}", principal != null ? principal.getName() : "NULL");
                userId = extractUserIdFromPrincipal(principal, headers);
                if (userId != null) {
                    userPresenceService.setUserOnline(userId);
                    log.info("WebSocket 연결: 사용자 {} 온라인 상태 설정 (Redis, fallback)", userId);
                }
            }
            
            if (userId == null) {
                log.warn("WebSocket 연결: 사용자 ID를 찾을 수 없음 - SessionId: {}", headers.getSessionId());
            }
            
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
            
            // 1. 세션 속성에서 우선 추출 (가장 확실한 방법)
            Long userId = extractUserIdFromSessionAttributes(headers);
            log.debug("WebSocket 해제 이벤트 - SessionAttributes에서 userId 조회: {}", userId);
            if (headers.getSessionAttributes() == null) {
                log.debug("WebSocket 해제 이벤트 - SessionAttributes가 null");
            }
            
            if (userId != null) {
                userPresenceService.setUserOffline(userId);
                log.info("WebSocket 해제: 사용자 {} 오프라인 상태 설정 (Redis)", userId);
            } else {
                // 2. Principal에서 시도 (fallback)
                Principal principal = headers.getUser();
                log.debug("WebSocket 해제 이벤트 - Principal: {}", principal != null ? principal.getName() : "NULL");
                userId = extractUserIdFromPrincipal(principal, headers);
                if (userId != null) {
                    userPresenceService.setUserOffline(userId);
                    log.info("WebSocket 해제: 사용자 {} 오프라인 상태 설정 (Redis, fallback)", userId);
                }
            }
            
            if (userId == null) {
                log.warn("WebSocket 해제: 사용자 ID를 찾을 수 없음 - SessionId: {}", headers.getSessionId());
            }
            
        } catch (Exception e) {
            log.error("WebSocket 해제 처리 중 오류 발생", e);
        }
    }

    /**
     * Principal과 세션 속성에서 사용자 ID 추출하는 헬퍼 메소드
     * NotificationController와 동일한 방식으로 처리
     */
    private Long extractUserIdFromPrincipal(Principal principal, StompHeaderAccessor headers) {
        log.debug("Principal 타입: {}, 값: {}", 
            principal != null ? principal.getClass().getSimpleName() : "NULL", 
            principal != null ? principal.getName() : "NULL");
        
        // 1. 세션 속성에서 직접 추출 (가장 확실한 방법 - 알림과 동일)
        if (headers != null && headers.getSessionAttributes() != null) {
            Object userId = headers.getSessionAttributes().get("userId");
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
            log.debug("Principal.getName()을 userId로 파싱할 수 없음 (이메일 형식): {}", principal.getName());
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
    
    /**
     * 세션 속성에서 사용자 ID 추출 (채팅과 동일한 방식)
     */
    private Long extractUserIdFromSessionAttributes(StompHeaderAccessor headers) {
        if (headers.getSessionAttributes() != null) {
            Object userId = headers.getSessionAttributes().get("userId");
            if (userId instanceof Long) {
                log.info("세션 속성에서 userId 추출 성공: {}", userId);
                return (Long) userId;
            }
        }
        return null;
    }
}