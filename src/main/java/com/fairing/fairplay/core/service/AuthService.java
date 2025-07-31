package com.fairing.fairplay.core.service;

import com.fairing.fairplay.core.dto.LoginRequest;
import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.core.service.RefreshTokenService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
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
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String redisToken = refreshTokenService.getRefreshToken(userId);

        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token not found");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
