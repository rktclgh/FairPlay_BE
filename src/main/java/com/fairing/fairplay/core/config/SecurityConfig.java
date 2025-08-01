package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.security.JwtAuthenticationFilter;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/users/signup",   // 회원가입 (UserController)
                                "/api/auth/login",     // 로그인
                                "/api/auth/logout",   //로그아웃
                                "/api/auth/refresh",   // 토큰 재발급
                                "/api/users/forgot-password",//임시 비밀번호 발급
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/users/check-email",
                                "/api/users/check-nickname",
                                "/api/email/verify-code",
                                "/api/email/send-verification",
                                "/api/auth/kakao"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
