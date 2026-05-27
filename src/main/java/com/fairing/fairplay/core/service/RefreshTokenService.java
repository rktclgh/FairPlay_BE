package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final SessionService sessionService;

    @Value("${jwt.refresh-token-prefix:refresh_token:}")
    private String prefix = "refresh_token:";

    private static final DefaultRedisScript<Long> SAVE_IF_AUTH_CURRENT_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('exists', KEYS[2]) == 1 then
              return 0
            end
            local revokedAt = redis.call('get', KEYS[3])
            if revokedAt and tonumber(ARGV[2]) <= tonumber(revokedAt) then
              return 0
            end
            redis.call('psetex', KEYS[1], ARGV[1], ARGV[3])
            return 1
            """,
            Long.class
    );

    public void saveRefreshTokenIfAuthCurrent(Long userId, String refreshToken, long expiryMs, Long authenticatedAt) {
        long issuedAt = authenticatedAt != null ? authenticatedAt : System.currentTimeMillis();
        Long saved = redisTemplate.execute(
                SAVE_IF_AUTH_CURRENT_SCRIPT,
                List.of(
                        prefix + userId,
                        sessionService.userSessionsBlockedKey(userId),
                        sessionService.userSessionsRevokedAtKey(userId)
                ),
                String.valueOf(expiryMs),
                String.valueOf(issuedAt),
                refreshToken
        );
        if (!Long.valueOf(1L).equals(saved)) {
            throw new CustomException(HttpStatus.CONFLICT, "계정 상태가 변경되었습니다. 다시 로그인해주세요.");
        }
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
