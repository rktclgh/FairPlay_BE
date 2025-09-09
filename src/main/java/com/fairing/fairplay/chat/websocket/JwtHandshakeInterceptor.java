package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.UserPresenceService;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.fairing.fairplay.core.security.CustomUserDetails;

import java.security.Principal;
import java.util.Map;
import jakarta.servlet.http.HttpSession;


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
        
        // ================================= HTTP-only 쿠키 기반 인증으로 변경 =================================
        try {
            // 1. HTTP 세션에서 사용자 정보 추출
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                HttpSession session = servletRequest.getServletRequest().getSession(false);
                
                if (session != null) {
                    // 세션에서 SecurityContext 추출
                    SecurityContext securityContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
                    if (securityContext != null) {
                        Authentication authentication = securityContext.getAuthentication();
                        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                            Long userId = userDetails.getUserId();
                            
                            // WebSocket 세션에 사용자 정보 설정
                            attributes.put("user", new com.fairing.fairplay.chat.websocket.StompPrincipal(userId.toString()));
                            attributes.put("userId", userId);
                            
                            // 사용자를 온라인 상태로 설정 (Redis)
                            userPresenceService.setUserOnline(userId);
                            log.info("WebSocket 인증 성공 (HTTP 세션): 사용자 ID {} 설정", userId);
                            return true;
                        }
                    }
                }
            }
            
            // 2. Fallback: JWT 토큰 방식 (기존 로직 유지)
            String token = extractToken(request);
            log.debug("WebSocket handshake - URI: {}, Token: {}", request.getURI(), token != null ? "EXISTS" : "NULL");
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                attributes.put("user", new com.fairing.fairplay.chat.websocket.StompPrincipal(userId.toString()));
                attributes.put("userId", userId);
                
                // 사용자를 온라인 상태로 설정 (Redis)
                userPresenceService.setUserOnline(userId);
                log.info("WebSocket 인증 성공 (JWT): 사용자 ID {} 설정", userId);
                return true;
            }
            
            log.debug("WebSocket 연결 실패: 인증 정보 없음 (HTTP 세션 및 JWT 토큰 모두 없음)");
            return false;
            
        } catch (Exception e) {
            log.warn("WebSocket 연결 실패: 인증 처리 중 오류 - {}", e.getMessage());
            return false;
        }
        // ===============================================================================================
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
