package com.fairing.fairplay.chat.listener;

import com.fairing.fairplay.chat.service.UserPresenceService;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.entity.Users;
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
    private final UserRepository userRepository;
    
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
     * HTTP-only 쿠키 기반 인증 방식에 최적화됨
     */
    private Long extractUserIdFromPrincipal(Principal principal, StompHeaderAccessor headers) {
        log.debug("Principal 타입: {}, 값: {}", 
            principal != null ? principal.getClass().getSimpleName() : "NULL", 
            principal != null ? principal.getName() : "NULL");
        
        // 1. 세션 속성에서 직접 추출 (HTTP-only 쿠키 방식에서 가장 확실한 방법)
        if (headers != null && headers.getSessionAttributes() != null) {
            Object userId = headers.getSessionAttributes().get("userId");
            log.debug("세션 속성에서 userId 조회: {}", userId);
            if (userId instanceof Long) {
                log.info("✅ 세션 속성에서 userId 추출 성공: {}", userId);
                return (Long) userId;
            }
        }
        
        // 2. Principal이 이메일 형식인 경우 DB에서 userId 조회
        if (principal != null && principal.getName() != null) {
            String principalName = principal.getName();
            if (principalName.contains("@")) {
                log.debug("Principal이 이메일 형식: {}, DB에서 userId 조회 시도", principalName);
                try {
                    Users user = userRepository.findByEmail(principalName).orElse(null);
                    if (user != null) {
                        log.info("✅ 이메일로 userId 조회 성공: {} -> {}", principalName, user.getUserId());
                        return user.getUserId();
                    } else {
                        log.warn("❌ 이메일로 사용자를 찾을 수 없음: {}", principalName);
                    }
                } catch (Exception e) {
                    log.error("❌ 이메일로 userId 조회 중 오류: {}", principalName, e);
                }
            }
        }
        
        log.warn("❌ Principal에서 userId를 추출할 수 없음. Principal: {}", 
            principal != null ? principal.getName() : "NULL");
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