package com.fairing.fairplay.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;
    // key: "RT:{userId}", value: refreshToken

    private String getRefreshTokenKey(Long userId) {
        return "RT:" + userId;
    }

    // 리프레시 토큰 저장 (만료시간 설정)
    public void saveRefreshToken(Long userId, String refreshToken, long durationMillis) {
        String key = getRefreshTokenKey(userId);
        redisTemplate.opsForValue().set(key, refreshToken, durationMillis, TimeUnit.MILLISECONDS);
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(Long userId) {
        String key = getRefreshTokenKey(userId);
        return redisTemplate.opsForValue().get(key);
    }

    // 리프레시 토큰 삭제 (로그아웃, 만료 등)
    public void deleteRefreshToken(Long userId) {
        String key = getRefreshTokenKey(userId);
        redisTemplate.delete(key);
    }

    // 리프레시 토큰 일치 여부 검증
    public boolean isValidRefreshToken(Long userId, String refreshToken) {
        String stored = getRefreshToken(userId);
        return stored != null && stored.equals(refreshToken);
    }
}
