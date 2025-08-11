package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.security.JwtAuthenticationFilter;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;
        private final UserRepository userRepository;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", // 루트 경로 (index.html)
                                                                "/index.html", // 메인 정적 페이지
                                                                "/assets/**", // vite 등 빌드 시 산출물
                                                                "/images/**", // 이미지 리소스(필요시)
                                                                "/favicon.ico",
                                                                "/static/**", // 혹시 모듈/서브폴더 등
                                                                "/manifest.json", // PWA/프론트 프로젝트일 때
                                                                "/robots.txt",
                                                                "/api/users/signup",
                                                                "/api/auth/login",
                                                                "/api/auth/logout",
                                                                "/api/auth/refresh",
                                                                "/api/events/**",
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
                                                                "/api/super-admin/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
