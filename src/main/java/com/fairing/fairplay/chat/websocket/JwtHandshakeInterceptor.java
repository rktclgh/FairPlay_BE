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
        String token = null;
        if (request.getHeaders().containsKey("Authorization")) {
            String auth = request.getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        } else {
            String uri = request.getURI().toString();
            int idx = uri.indexOf("token=");
            if (idx > 0) {
                token = uri.substring(idx + 6);
            }
        }
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            attributes.put("user", new StompPrincipal(userId.toString()));
            attributes.put("userId", userId);
            
            // 사용자를 온라인 상태로 설정 (Redis)
            userPresenceService.setUserOnline(userId);
            log.debug("WebSocket 연결 성공: 사용자 {}", userId);
            return true;
        }
        
        // 토큰이 없는 경우는 로그를 남기지 않음 (인증되지 않은 사용자)
        if (token == null) {
            return false;
        }
        
        // 토큰은 있지만 유효하지 않은 경우만 DEBUG 레벨로 로그 출력
        log.debug("WebSocket 연결 실패: 토큰 만료 또는 유효하지 않음");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
