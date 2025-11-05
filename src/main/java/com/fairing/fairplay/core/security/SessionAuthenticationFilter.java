package com.fairing.fairplay.core.security;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Redis 세션 기반 인증 필터
 * HTTP-only 쿠키에서 sessionId를 추출하여 사용자 인증 처리
 */
@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    private static final String SESSION_COOKIE_NAME = "FAIRPLAY_SESSION";

    // DB 조회가 불필요한 공개 경로 패턴
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/creators",
        "/api/events",
        "/api/banners",
        "/api/reviews",
        "/api/calendar",
        "/static/",
        "/assets/",
        "/images/",
        "/favicon.ico",
        "/index.html",
        "/api/auth/",
        "/api/users/signup",
        "/api/users/check-",
        "/api/email/",
        "/ws/",
        "/api/qr-tickets/",
        "/api/uploads/",
        "/uploads/"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String sessionId = resolveSessionId(request);
        log.debug("요청 URI: {}, sessionId: {}", requestURI, sessionId);

        // 세션이 없고 공개 경로인 경우 DB 조회 스킵
        if (sessionId == null || isPublicPath(requestURI)) {
            if (sessionId == null) {
                log.debug("세션 없음 - 공개 경로 접근: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (sessionId != null) {
            Map<String, Object> sessionData = sessionService.getSessionData(sessionId);
            
            if (sessionData != null) {
                Object userIdObj = sessionData.get("userId");
                if (userIdObj instanceof Number) {
                    Long userId = ((Number) userIdObj).longValue();

                    // DB에서 사용자 정보 조회 (탈퇴 여부 확인)
                    Users user = userRepository.findById(userId).orElse(null);

                    if (user != null && user.getDeletedAt() == null) {
                        // CustomUserDetails 생성
                        CustomUserDetails userDetails = new CustomUserDetails(user);

                        // Spring Security 인증 컨텍스트 설정
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("세션 인증 성공 - sessionId: {}, userId: {}", sessionId, userId);
                    } else {
                        log.warn("유효하지 않은 사용자 - userId: {}", userId);
                        // 세션 삭제
                        sessionService.deleteSession(sessionId);
                    }
                }
            } else {
                log.debug("세션을 찾을 수 없음 - sessionId: {}", sessionId);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 sessionId 추출
     */
    private String resolveSessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    log.debug("FAIRPLAY_SESSION 쿠키 발견: {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 공개 경로 여부 확인 (GET 요청만)
     */
    private boolean isPublicPath(String requestURI) {
        return PUBLIC_PATHS.stream().anyMatch(requestURI::startsWith);
    }
}