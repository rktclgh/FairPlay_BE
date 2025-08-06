package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.core.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

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
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
