package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.UserPresenceService;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

// Principal 구현
class StompPrincipal implements Principal {
    private final String name;
    public StompPrincipal(String name) { this.name = name; }
    @Override public String getName() { return name; }
}

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatPresenceService chatPresenceService;
    private final UserPresenceService userPresenceService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        log.debug("WebSocket handshake - URI: {}, Token: {}", request.getURI(), token != null ? "EXISTS" : "NULL");
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                Long userId = jwtTokenProvider.getUserId(token);
                attributes.put("user", new StompPrincipal(userId.toString()));
                attributes.put("userId", userId);
                
                // 사용자를 온라인 상태로 설정 (Redis)
                userPresenceService.setUserOnline(userId);
                log.info("WebSocket 인증 성공: 사용자 ID {} 설정", userId);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket 연결 실패: 토큰에서 사용자 ID 추출 실패 - {}", e.getMessage());
                return false;
            }
        }
        
        // 토큰이 없는 경우는 조용히 거부 (로그인하지 않은 사용자는 정상)
        if (token == null) {
            log.debug("WebSocket 연결 실패: 토큰 없음");
            return false;
        }
        
        // 토큰이 유효하지 않은 경우 DEBUG 레벨로 로그
        log.debug("WebSocket 연결 실패: 토큰 만료 또는 유효하지 않음");
        return false;
    }
    
    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. Authorization 헤더에서 추출
        if (request.getHeaders().containsKey("Authorization")) {
            String auth = request.getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                return auth.substring(7);
            }
        }
        
        // 2. 쿼리 매개변수에서 추출
        String uri = request.getURI().toString();
        int idx = uri.indexOf("token=");
        if (idx >= 0) {
            String tokenPart = uri.substring(idx + 6);
            // & 또는 # 문자까지만 토큰으로 인식
            int endIdx = tokenPart.indexOf('&');
            if (endIdx == -1) {
                endIdx = tokenPart.indexOf('#');
            }
            if (endIdx != -1) {
                return tokenPart.substring(0, endIdx);
            }
            return tokenPart;
        }
        
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
