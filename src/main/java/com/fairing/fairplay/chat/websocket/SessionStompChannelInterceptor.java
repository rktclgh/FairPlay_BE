package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.core.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStompChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> PUBLIC_SUBSCRIBE_PREFIXES = Set.of(
            "/topic/check-in/",
            "/topic/check-out/",
            "/topic/booth/qr/",
            "/topic/waiting/");

    private final SessionService sessionService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            log.debug("STOMP Message - Command: {}", accessor.getCommand());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String sessionId = extractSessionIdFromHeaders(accessor);
                log.debug("STOMP CONNECT - SessionId: {}", sessionId != null ? "EXISTS" : "NULL");
                
                if (sessionId != null) {
                    Long userId = sessionService.getUserIdFromSession(sessionId);
                    if (userId != null) {
                        accessor.setUser(new StompPrincipal(userId.toString()));
                        
                        if (accessor.getSessionAttributes() != null) {
                            accessor.getSessionAttributes().put("userId", userId);
                            log.debug("STOMP CONNECT - 사용자 ID {} 설정 완료", userId);
                        } else {
                            log.debug("STOMP CONNECT - SessionAttributes가 null");
                        }
                    } else {
                        log.debug("STOMP CONNECT - 세션에서 사용자 ID 조회 실패");
                    }
                } else {
                    log.debug("STOMP CONNECT - 세션 ID 없음");
                }
            }

            enforceDestinationAccess(accessor);
        }
        
        return message;
    }

    private void enforceDestinationAccess(StompHeaderAccessor accessor) {
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return;
        }

        String destination = accessor.getDestination();
        if (StompCommand.SEND.equals(command) && destination != null && destination.startsWith("/app/")) {
            requireAuthenticated(accessor, command, destination);
            return;
        }

        if (StompCommand.SUBSCRIBE.equals(command) && destination != null && !isPublicSubscribeDestination(destination)) {
            requireAuthenticated(accessor, command, destination);
        }
    }

    private boolean isPublicSubscribeDestination(String destination) {
        return PUBLIC_SUBSCRIBE_PREFIXES.stream().anyMatch(destination::startsWith);
    }

    private void requireAuthenticated(StompHeaderAccessor accessor, StompCommand command, String destination) {
        if (authenticatedUserId(accessor) != null) {
            return;
        }
        log.warn("STOMP {} denied for unauthenticated destination {}", command, destination);
        throw new AccessDeniedException("Authentication is required for " + destination);
    }

    private Long authenticatedUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                log.debug("STOMP principal is not a numeric user id: {}", principal.getName());
            }
        }

        if (accessor.getSessionAttributes() == null) {
            return null;
        }
        Object userId = accessor.getSessionAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private String extractSessionIdFromHeaders(StompHeaderAccessor accessor) {
        String sessionId = accessor.getFirstNativeHeader("sessionId");
        if (sessionId != null) {
            log.debug("Found sessionId header: {}", sessionId);
            return sessionId;
        }
        
        String cookie = accessor.getFirstNativeHeader("Cookie");
        log.debug("STOMP Cookie Header: {}", cookie);
        
        if (cookie != null) {
            String[] cookies = cookie.split(";");
            for (String c : cookies) {
                String[] parts = c.trim().split("=", 2);
                if (parts.length == 2 && "FAIRPLAY_SESSION".equals(parts[0])) {
                    log.debug("Found FAIRPLAY_SESSION in STOMP: {}", parts[1]);
                    return parts[1];
                }
            }
        }
        
        log.debug("FAIRPLAY_SESSION not found in STOMP headers");
        return null;
    }
}
