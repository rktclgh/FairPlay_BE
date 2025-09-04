package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.UserPresenceService;
import com.fairing.fairplay.core.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.HttpCookie;
import java.security.Principal;
import java.util.List;
import java.util.Map;

class StompPrincipal implements Principal {
    private final String name;
    public StompPrincipal(String name) { this.name = name; }
    @Override public String getName() { return name; }
}

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    private final SessionService sessionService;
    private final ChatPresenceService chatPresenceService;
    private final UserPresenceService userPresenceService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String sessionId = extractSessionId(request);
        log.debug("WebSocket handshake - URI: {}, SessionId: {}", request.getURI(), sessionId != null ? "EXISTS" : "NULL");
        
        if (sessionId != null) {
            Long userId = sessionService.getUserIdFromSession(sessionId);
            if (userId != null) {
                attributes.put("user", new StompPrincipal(userId.toString()));
                attributes.put("userId", userId);
                
                userPresenceService.setUserOnline(userId);
                log.debug("WebSocket 인증 성공: 사용자 ID {} 설정", userId);
                return true;
            }
        }
        
        log.debug("WebSocket 연결 실패: 세션 없음 또는 만료됨");
        return false;
    }
    
    private String extractSessionId(ServerHttpRequest request) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        log.debug("WebSocket Cookie Header: {}", cookieHeader);
        
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "FAIRPLAY_SESSION".equals(parts[0])) {
                    log.debug("Found FAIRPLAY_SESSION cookie: {}", parts[1]);
                    return parts[1];
                }
            }
        }
        
        log.debug("FAIRPLAY_SESSION cookie not found");
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}