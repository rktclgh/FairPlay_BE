package com.fairing.fairplay.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-prefix:refresh_token:}")
    private String prefix;

    public void saveRefreshToken(Long userId, String refreshToken, long expiryMs) {
        String key = prefix + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expiryMs, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(Long userId) {
        String key = prefix + userId;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(Long userId) {
        String key = prefix + userId;
        redisTemplate.delete(key);
    }
}
