package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.dto.LoginRequest;
import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // 로그인 + JWT 발급
    public LoginResponse login(LoginRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getDeletedAt() != null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRoleCode().getName()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );

        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiry()
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    // 리프레시 토큰 재발급
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String redisToken = refreshTokenService.getRefreshToken(userId);

        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다.");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getDeletedAt() != null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRoleCode().getName()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );

        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiry()
        );

        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}
