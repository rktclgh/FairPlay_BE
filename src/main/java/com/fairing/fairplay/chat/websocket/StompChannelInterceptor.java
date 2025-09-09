package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.core.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            log.debug("STOMP Message - Command: {}, Headers: {}", accessor.getCommand(), accessor.toNativeHeaderMap());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // ================================= HTTP-only 쿠키 기반 인증 우선 처리 =================================
                // HandshakeInterceptor에서 이미 userId를 설정했는지 확인
                if (accessor.getSessionAttributes() != null) {
                    Object existingUserId = accessor.getSessionAttributes().get("userId");
                    if (existingUserId instanceof Long) {
                        Long userId = (Long) existingUserId;
                        accessor.setUser(new StompPrincipal(userId.toString()));
                        log.info("STOMP CONNECT - HandshakeInterceptor에서 설정된 사용자 ID {} 사용", userId);
                        return message; // 이미 설정되어 있으므로 추가 처리 불필요
                    }
                }
                
                // Fallback: JWT 토큰 방식 (기존 로직)
                String token = extractTokenFromHeaders(accessor);
                log.info("STOMP CONNECT - Token: {}", token != null ? "EXISTS" : "NULL");
                log.info("STOMP CONNECT - All Native Headers: {}", accessor.toNativeHeaderMap());
                
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    try {
                        Long userId = jwtTokenProvider.getUserId(token);
                        accessor.setUser(new StompPrincipal(userId.toString()));
                        
                        // SessionAttributes에 직접 설정
                        if (accessor.getSessionAttributes() != null) {
                            accessor.getSessionAttributes().put("userId", userId);
                            log.info("STOMP CONNECT - 사용자 ID {} 설정 완료 (JWT)", userId);
                        } else {
                            log.warn("STOMP CONNECT - SessionAttributes가 null");
                        }
                    } catch (Exception e) {
                        log.warn("STOMP CONNECT - 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
                    }
                } else {
                    log.debug("STOMP CONNECT - 유효하지 않은 토큰 또는 토큰 없음 (HTTP 세션으로 이미 인증되었을 수 있음)");
                }
                // ===============================================================================================
            }
        }
        
        return message;
    }

    private String extractTokenFromHeaders(StompHeaderAccessor accessor) {
        // 1. Authorization 헤더에서 토큰 추출
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        
        // 2. token 헤더에서 직접 추출
        String token = accessor.getFirstNativeHeader("token");
        if (token != null) {
            return token;
        }
        
        return null;
    }
}