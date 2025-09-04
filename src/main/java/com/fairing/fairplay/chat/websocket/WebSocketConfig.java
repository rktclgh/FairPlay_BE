package com.fairing.fairplay.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SessionHandshakeInterceptor sessionHandshakeInterceptor;
    private final SessionStompChannelInterceptor sessionStompChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 (SockJS 없이)
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(sessionHandshakeInterceptor);

        // SockJS fallback 엔드포인트 (JSONP 비활성화)
        registry.addEndpoint("/ws/chat-sockjs")
                .setAllowedOriginPatterns("http://localhost:5173", "https://fair-play.ink")
                .addInterceptors(sessionHandshakeInterceptor)
                .withSockJS()
                .setSessionCookieNeeded(true)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000);

        // 알림 전용 WebSocket 엔드포인트
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(sessionHandshakeInterceptor);

        // 알림 전용 SockJS fallback 엔드포인트
        registry.addEndpoint("/ws/notifications-sockjs")
                .setAllowedOriginPatterns("http://localhost:5173", "https://fair-play.ink")
                .addInterceptors(sessionHandshakeInterceptor)
                .withSockJS()
                .setSessionCookieNeeded(true)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000);

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
        registration.interceptors(sessionStompChannelInterceptor);
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