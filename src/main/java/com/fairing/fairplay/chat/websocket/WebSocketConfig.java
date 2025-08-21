package com.fairing.fairplay.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompChannelInterceptor stompChannelInterceptor;
    private final Environment environment;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        boolean isLocalProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");

        // WebSocket 엔드포인트 (SockJS 없이)
        var chatEndpoint = registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*");

        // SockJS fallback 엔드포인트 (JSONP 비활성화)
        var chatSockJsEndpoint = registry.addEndpoint("/ws/chat-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000)
                .setSuppressCors(false);

        // 알림 전용 WebSocket 엔드포인트
        var notificationsEndpoint = registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*");

        // 알림 전용 SockJS fallback 엔드포인트
        var notificationsSockJsEndpoint = registry.addEndpoint("/ws/notifications-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000)
                .setSuppressCors(false);

        // local 프로파일이 아닐 때만 인터셉터 추가
        if (!isLocalProfile) {
            chatEndpoint.addInterceptors(jwtHandshakeInterceptor);
            notificationsEndpoint.addInterceptors(jwtHandshakeInterceptor);
        }

        // 체크인/체크아웃 전용 SockJS fallback 엔드포인트
        registry.addEndpoint("/ws/qr-sockjs")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
            .setHeartbeatTime(25000)
            .setDisconnectDelay(30000)
            .setSuppressCors(false);

        // 부스 체크인 전용 SockJS fallback 엔드포인트
        registry.addEndpoint("/ws/booth-sockjs")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
            .setHeartbeatTime(25000)
            .setDisconnectDelay(30000)
            .setSuppressCors(false);

        // 부스 실시간 웨이팅 전용 SockJS fallback 엔드포인트
        registry.addEndpoint("/ws/waiting-sockjs")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
            .setHeartbeatTime(25000)
            .setDisconnectDelay(30000)
            .setSuppressCors(false);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{25000, 25000}) // heartbeat 25초
                .setTaskScheduler(heartBeatScheduler()); // TaskScheduler 추가
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        boolean isLocalProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (!isLocalProfile) {
            registration.interceptors(stompChannelInterceptor);
        }
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
