package com.fairing.fairplay.core.security;

import com.fairing.fairplay.core.service.SessionService;
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

    private static final String SESSION_COOKIE_NAME = "FAIRPLAY_SESSION";

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/creators",
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
        "/api/qr-tickets/",
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

        if (sessionId == null) {
            log.debug("세션 없음 - 인증 필터 통과: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        if (shouldSkipAuthentication(request.getMethod(), requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        Map<String, Object> sessionData = sessionService.getSessionData(sessionId);
            
        if (sessionData != null) {
            Object userIdObj = sessionData.get("userId");
            if (userIdObj instanceof Number) {
                Long userId = ((Number) userIdObj).longValue();
                String email = stringValue(sessionData.get("email"));
                String roleCode = stringValue(sessionData.get("role"));
                Number roleId = numberValue(sessionData.get("roleId"));

                if (roleCode != null) {
                    CustomUserDetails userDetails = CustomUserDetails.fromSession(userId, email, roleCode, roleId);

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
                    log.warn("세션 role 정보 누락 - userId: {}", userId);
                    sessionService.deleteSession(sessionId);
                }
            }
        } else {
            log.debug("세션을 찾을 수 없음 - sessionId: {}", sessionId);
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
    private boolean shouldSkipAuthentication(String method, String requestURI) {
        return "GET".equalsIgnoreCase(method)
                && (PUBLIC_PATHS.stream().anyMatch(requestURI::startsWith)
                || isPublicEventGetPath(requestURI));
    }

    private boolean isPublicEventGetPath(String requestURI) {
        if ("/api/events".equals(requestURI)
                || "/api/events/hot-picks".equals(requestURI)
                || "/api/events/apply/check".equals(requestURI)) {
            return true;
        }

        return requestURI.matches("^/api/events/[^/]+/details$")
                || requestURI.matches("^/api/events/[^/]+/booths(?:/.*)?$")
                || requestURI.matches("^/api/events/[^/]+/schedule$")
                || requestURI.matches("^/api/events/[^/]+/tickets$")
                || requestURI.matches("^/api/events/schedule/[^/]+/tickets$");
    }

    private String stringValue(Object value) {
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    private Number numberValue(Object value) {
        return value instanceof Number number ? number : null;
    }
}
