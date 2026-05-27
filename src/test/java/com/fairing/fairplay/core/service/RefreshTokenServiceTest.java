package com.fairing.fairplay.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SessionService sessionService;

    @Test
    @SuppressWarnings("unchecked")
    void saveRefreshTokenIfAuthCurrentChecksRevocationAndStoresAtomically() {
        when(sessionService.userSessionsBlockedKey(42L)).thenReturn("user_sessions_blocked:42");
        when(sessionService.userSessionsRevokedAtKey(42L)).thenReturn("user_sessions_revoked_at:42");
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(List.class),
                eq("60000"),
                eq("1000"),
                eq("refresh-token")
        )).thenReturn(1L);

        new RefreshTokenService(redisTemplate, sessionService)
                .saveRefreshTokenIfAuthCurrent(42L, "refresh-token", 60000L, 1000L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                keysCaptor.capture(),
                eq("60000"),
                eq("1000"),
                eq("refresh-token")
        );
        assertThat(keysCaptor.getValue()).containsExactly(
                "refresh_token:42",
                "user_sessions_blocked:42",
                "user_sessions_revoked_at:42"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveRefreshTokenIfAuthCurrentFailsWhenRevocationGuardRejects() {
        when(sessionService.userSessionsBlockedKey(42L)).thenReturn("user_sessions_blocked:42");
        when(sessionService.userSessionsRevokedAtKey(42L)).thenReturn("user_sessions_revoked_at:42");
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(List.class),
                eq("60000"),
                eq("1000"),
                eq("refresh-token")
        )).thenReturn(0L);

        assertThatThrownBy(() -> new RefreshTokenService(redisTemplate, sessionService)
                .saveRefreshTokenIfAuthCurrent(42L, "refresh-token", 60000L, 1000L))
                .hasMessageContaining("계정 상태가 변경");
    }
}
