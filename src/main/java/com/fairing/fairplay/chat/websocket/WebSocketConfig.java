package com.fairing.fairplay.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.*;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // JSONP transport를 제외한 transport handlers 정의
        List<TransportHandler> transports = Arrays.asList(
            new WebSocketTransportHandler(new DefaultHandshakeHandler()),
            new XhrPollingTransportHandler(),
            new XhrReceivingTransportHandler(),
            new XhrStreamingTransportHandler()
            // JsonpPollingTransportHandler와 JsonpReceivingTransportHandler 제외
        );

        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)  // 25초마다 heartbeat
                .setDisconnectDelay(30000) // 30초 disconnect delay
                .setTransportHandlers(transports); // JSONP transport 비활성화

        // 알림 전용 WebSocket 엔드포인트 추가  
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000)
                .setTransportHandlers(transports); // JSONP transport 비활성화
                
        // 순수 WebSocket 엔드포인트도 추가 (SockJS 없이)
        registry.addEndpoint("/ws/chat-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{25000, 25000}) // heartbeat 25초
                .setTaskScheduler(heartBeatScheduler()); // TaskScheduler 추가
        config.setApplicationDestinationPrefixes("/app");
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
