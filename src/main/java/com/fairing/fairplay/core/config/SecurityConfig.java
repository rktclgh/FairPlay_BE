package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.security.JwtAuthenticationFilter;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;
        private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/ws/chat/**") // ★ 반드시 추가
                        .disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",                // 루트 경로 (index.html)
                                "/index.html",      // 메인 정적 페이지
                                "/assets/**",
                                "/images/**",
                                "/favicon.ico",
                                "/api/calendar/**",
                                "/static/**",
                                "/manifest.json",
                                "/robots.txt",
                                "/api/users/signup",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/refresh",
                                "/api/events",                     // GET 행사 목록 조회
                                "/api/events/*/details",           // GET 행사 상세 조회 (*/details 패턴)
                                                                "/api/users/forgot-password",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/api/users/check-email",
                                                                "/api/users/check-nickname",
                                                                "/api/email/verify-code",
                                                                "/api/email/send-verification",
                                                                "/api/auth/kakao",
                                                                "/auth/kakao/callback",
                                                                "/api/users/event-admin/*/public",
                                                                "/api/qr-tickets/*",
                                                                "/api/qr-tickets/reissue",
                                                                "/ws/**", // ★ 반드시 필요
                                                "/ws/*/info", // SockJS info 엔드포인트
                                                                "/api/chat/rooms/**", // 채팅방 목록 조회만 허용
                                "/api/chat/presence/status/**", // 사용자 온라인 상태 조회 허용
                                "/api/uploads/**",
                                "/api/payments/complete",  // PG사에서 호출하는 결제 완료 웹훅
                                "/api/events/apply", // 행사 등록 신청
                                "/api/events/apply/check",
                                "/api/events/user/role",
                                "/api/super-admin/**",
                                "api/qr-tickets/reissue/guest",
                                "/api/rag/**", // RAG API (개발/테스트용)
                                "/api/qr-tickets/admin/issue" // QR 티켓 강제 재발급 (테스트용)
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/form").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/attendees").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/schedule").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events//schedule/*/tickets").permitAll()
                        .requestMatchers("/api/chat/presence/connect",
                                "/api/chat/presence/disconnect")
                        .authenticated() // JWT 인증
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
