package com.fairing.fairplay.core.controller;

import com.fairing.fairplay.core.dto.KakaoLoginRequest;
import com.fairing.fairplay.core.dto.LoginRequest;
import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.dto.RefreshTokenRequest;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.service.AuthService;
import com.fairing.fairplay.core.service.RefreshTokenService;
import com.fairing.fairplay.core.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    
    private static final String SESSION_COOKIE_NAME = "FAIRPLAY_SESSION";
    private static final int COOKIE_MAX_AGE = 7200; // 2시간

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        
        // Redis 세션 생성
        String sessionId = sessionService.createSession(
            response.getUserId(), 
            response.getEmail(), 
            response.getRole(), 
            response.getRoleId()
        );
        
        // HTTP-only 쿠키 설정
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // 로컬 개발환경에서는 false
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(COOKIE_MAX_AGE);
        
        log.debug("세션 쿠키 설정: sessionId={}", sessionId);
        // sessionCookie.setSameSite(Cookie.SameSite.LAX); // Spring Boot 3.x
        
        httpResponse.addCookie(sessionCookie);
        
        // 응답에서 토큰 제거 (쿠키로만 전달)
        response.setAccessToken(null);
        response.setRefreshToken(null);
        
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, 
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 세션 쿠키에서 sessionId 추출
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            sessionService.deleteSession(sessionId);
        }
        
        // 기존 refresh token 삭제
        if (userDetails != null) {
            refreshTokenService.deleteRefreshToken(userDetails.getUserId());
        }
        
        // 쿠키 삭제
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, "");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // 로컬 개발환경에서는 false
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(sessionCookie);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody KakaoLoginRequest request, HttpServletResponse httpResponse) {
        LoginResponse response = authService.kakaoLogin(request.getCode());
        
        // Redis 세션 생성
        String sessionId = sessionService.createSession(
            response.getUserId(), 
            response.getEmail(), 
            response.getRole(), 
            response.getRoleId()
        );
        
        // HTTP-only 쿠키 설정
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // 로컬 개발환경에서는 false
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(COOKIE_MAX_AGE);
        
        log.debug("카카오 로그인 세션 쿠키 설정: sessionId={}", sessionId);
        
        httpResponse.addCookie(sessionCookie);
        
        // 응답에서 토큰 제거
        response.setAccessToken(null);
        response.setRefreshToken(null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 쿠키에서 sessionId 추출 헬퍼 메소드
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
