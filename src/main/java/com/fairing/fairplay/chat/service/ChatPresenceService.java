package com.fairing.fairplay.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final StringRedisTemplate redisTemplate;
    private static final long ONLINE_EXPIRE_SECONDS = 600; // 10분

    // key 예시: online:user:{userId} / online:manager:{userId}
    private String getKey(boolean isManager, Long userId) {
        return (isManager ? "online:manager:" : "online:user:") + userId;
    }

    public void setOnline(boolean isManager, Long userId) {
        redisTemplate.opsForValue().set(getKey(isManager, userId), "1", ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    public void setOffline(boolean isManager, Long userId) {
        redisTemplate.delete(getKey(isManager, userId));
    }

    public boolean isOnline(boolean isManager, Long userId) {
        String val = redisTemplate.opsForValue().get(getKey(isManager, userId));
        return "1".equals(val);
    }
}
