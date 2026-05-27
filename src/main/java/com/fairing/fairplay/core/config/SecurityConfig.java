package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.security.SessionAuthenticationFilter;
import com.fairing.fairplay.core.service.SessionService;
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

    private final SessionService sessionService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/ws/chat/**") // ★ 반드시 추가
                        .disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(
                                "/actuator/**",
                                "/api/admin/**",
                                "/api/super-admin/**",
                                "/api/rag/**",
                                "/api/qr-tickets/admin/**",
                                "/api/qr-tickets/test/**",
                                "/api/admin/reservation",
                                "/api/admin/reservation/**",
                                "/api/event-comparison",
                                "/api/event-comparison/**",
                                "/api/event-compare",
                                "/api/event-compare/**",
                                "/api/event-popularity",
                                "/api/event-popularity/**",
                                "/api/popular-events",
                                "/api/popular-events/**",
                                "/api/reservation-statistics",
                                "/api/reservation-statistics/**",
                                "/api/sales-statistics",
                                "/api/sales-statistics/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(
                                "/api/host/reservation",
                                "/api/host/reservation/**",
                                "/api/stats",
                                "/api/stats/**",
                                "/api/qr-tickets/check-in/**",
                                "/api/qr-tickets/check-out/**")
                        .hasAnyAuthority("ADMIN", "EVENT_MANAGER", "BOOTH_MANAGER")
                        .requestMatchers(
                                "/", // 루트 경로 (index.html)
                                "/index.html", // 메인 정적 페이지
                                "/assets/**",
                                "/images/**",
                                "/favicon.ico",
                                "/api/calendar/**",
                                "/static/**",
                                "/manifest.json",
                                "/robots.txt",
                                // 프론트엔드 라우트 허용
                                "/login",
                                "/register",
                                "/eventoverview",
                                "/event-registration-intro",
                                "/mypage/**",
                                "/admin_dashboard/**",
                                "/host/**",
                                "/booth-admin/**",
                                "/events/**",
                                "/eventdetail/**",
                                "/qr-ticket/**",
                                "/banner/payment",
                                "/auth/**",
                                "/api/users/signup",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/refresh",
                                "/api/events", // GET 행사 목록 조회
                                "/api/events/*/details", // GET 행사 상세 조회 (*/details 패턴)
                                "/api/events/hot-picks", // GET 핫픽 조회
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
                                "/uploads/**", // 로컬 파일 시스템의 정적 파일 서빙
                                "/api/payments/complete", // PG사에서 호출하는 결제 완료 웹훅
                                "/api/events/apply", // 행사 등록 신청
                                "/api/events/apply/check",
                                "/api/qr-tickets/reissue/guest",
                                "/api/events/*/booths/**",
                                "/api/events/{eventId}/booths/apply",
                                "/api/booths/cancel/**",
                                "/api/booths/payment/payment-page/**",
                                "/api/booths/payment/request-from-email",
                                "/api/booths/payment/complete",
                                "/api/banners/payment/payment-page/**",
                                "/api/banners/payment/request-from-email",
                                "/api/banners/payment/complete",
                                "/api/event-popularity/**",
                                "/api/banner/hot-picks",
                                "/api/banner/search-top",
                                "/api/admin/reservation/**",
                                "/api/event-compare/**",
                                "/api/popular-events/**",
                                "/api/sales-statistics/**",
                                "/api/reservation-statistics/**",
                                "/api/banners/hero/active",
                                "/api/event-compare/**",
                                "/api/popular-events/**",
                                "/api/reservation-statistics/**",
                                "/api/sales-statistics/**",
                                "/api/host/reservation/**",
                                "/participant-form",
                                "/participant-form/**",
                                "/booth/cancel",
                                "/booth/cancel/**",
                                // 제작자 페이지 - 공개 접근 허용
                                "/creators",
                                "/creators/**")
                        .permitAll()
                        // 제작자 API - GET 요청은 공개, 관리 기능은 ADMIN만
                        .requestMatchers(HttpMethod.GET, "/api/creators/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()

                                                .requestMatchers(HttpMethod.GET, "/api/reviews/*").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/form").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/attendees").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/events/*/schedule").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/events/*/tickets").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/events/schedule/*/tickets")
                                                .permitAll()
                                                .requestMatchers("/api/chat/presence/connect",
                                                                "/api/chat/presence/disconnect")
                                                .authenticated() // JWT 인증
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(401);
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\""
                                                                                        + authException.getMessage()
                                                                                        + "\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(403);
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Access Denied\",\"message\":\""
                                                                                        + accessDeniedException
                                                                                                        .getMessage()
                                                                                        + "\"}");
                                                }))
                                // 쿠키 기반 세션 인증 필터 추가
                                .addFilterBefore(
                                                new SessionAuthenticationFilter(sessionService),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
