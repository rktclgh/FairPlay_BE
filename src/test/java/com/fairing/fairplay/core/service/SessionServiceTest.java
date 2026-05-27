package com.fairing.fairplay.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final String USER_SESSION_INDEX_PREFIX = "user_sessions:";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(redisTemplate, new ObjectMapper());
    }

    @Test
    void createSessionRegistersSessionInUserReverseIndex() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        String sessionId = sessionService.createSession(42L, "admin@example.com", "ADMIN", 1L);

        verify(setOperations).add(USER_SESSION_INDEX_PREFIX + 42L, sessionId);
    }

    @Test
    void createSessionStoresAuthenticationTimestampInsteadOfSessionCreationTimestamp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        sessionService.createSession(42L, "admin@example.com", "ADMIN", 1L, 1000L);

        ArgumentCaptor<String> sessionJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(anyString(), sessionJsonCaptor.capture(), any(Duration.class));
        assertThat(sessionJsonCaptor.getValue()).contains("\"createdAt\":1000");
    }

    @Test
    void createSessionFailsWhenAuthenticationTimestampIsOlderThanRevocationMarker() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_sessions_revoked_at:42")).thenReturn("1000");

        assertThatThrownBy(() -> sessionService.createSession(42L, "admin@example.com", "ADMIN", 1L, 1000L))
                .hasMessageContaining("계정 상태가 변경");
    }

    @Test
    void deleteAllUserSessionsDeletesSessionIdMappedToUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(USER_SESSION_INDEX_PREFIX + 42L)).thenReturn(Set.of("session-a"));

        sessionService.deleteAllUserSessions(42L);

        verify(valueOperations).set(eq("user_sessions_revoked_at:42"), anyString(), any(Duration.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> deletedKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(deletedKeysCaptor.capture());
        assertThat(deletedKeysCaptor.getValue()).containsExactly("session:session-a");
        verify(redisTemplate).delete(USER_SESSION_INDEX_PREFIX + 42L);
    }

    @Test
    void deleteAllUserSessionsDeletesEverySessionIdWhenUserHasMultipleSessions() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(USER_SESSION_INDEX_PREFIX + 42L))
                .thenReturn(Set.of("session-a", "session-b", "session-c"));

        sessionService.deleteAllUserSessions(42L);

        verify(valueOperations).set(eq("user_sessions_revoked_at:42"), anyString(), any(Duration.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> deletedKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(deletedKeysCaptor.capture());
        assertThat(deletedKeysCaptor.getValue()).containsExactlyInAnyOrder(
                "session:session-a",
                "session:session-b",
                "session:session-c"
        );
        verify(redisTemplate).delete(USER_SESSION_INDEX_PREFIX + 42L);
    }

    @Test
    void deleteSessionRemovesSessionFromUserReverseIndex() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:session-a")).thenReturn("""
                {"userId":42,"email":"admin@example.com","role":"ADMIN","roleId":1,"createdAt":1000,"sessionVersion":2}
                """);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(USER_SESSION_INDEX_PREFIX + 42L)).thenReturn(0L);

        sessionService.deleteSession("session-a");

        verify(setOperations).remove(USER_SESSION_INDEX_PREFIX + 42L, "session-a");
        verify(redisTemplate).delete(USER_SESSION_INDEX_PREFIX + 42L);
        verify(redisTemplate).delete("session:session-a");
    }

    @Test
    void getSessionDataRejectsSessionCreatedBeforeUserRevocationMarker() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:session-a")).thenReturn("""
                {"userId":42,"email":"admin@example.com","role":"ADMIN","roleId":1,"createdAt":1000,"sessionVersion":2}
                """);
        when(valueOperations.get("user_sessions_revoked_at:42")).thenReturn("1000");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(sessionService.getSessionData("session-a")).isNull();

        verify(redisTemplate).delete("session:session-a");
    }

    @Test
    void getSessionDataRejectsLegacySessionWithoutSessionVersion() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:legacy-session")).thenReturn("""
                {"userId":42,"email":"admin@example.com","role":"ADMIN","roleId":1,"createdAt":1000}
                """);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(sessionService.getSessionData("legacy-session")).isNull();

        verify(redisTemplate).delete("session:legacy-session");
    }

    @Test
    void getSessionDataRejectsSessionWhileUserRevocationIsInProgress() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:session-a")).thenReturn("""
                {"userId":42,"email":"admin@example.com","role":"ADMIN","roleId":1,"createdAt":1000,"sessionVersion":2}
                """);
        when(redisTemplate.hasKey("user_sessions_blocked:42")).thenReturn(true);

        assertThat(sessionService.getSessionData("session-a")).isNull();
    }

    @Test
    void createSessionFailsClosedWhileUserRevocationIsInProgress() {
        when(redisTemplate.hasKey("user_sessions_blocked:42")).thenReturn(true);

        assertThatThrownBy(() -> sessionService.createSession(42L, "admin@example.com", "ADMIN", 1L))
                .hasMessageContaining("계정 상태 변경");
    }

    @Test
    void isValidSessionUsesRevocationChecks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:session-a")).thenReturn("""
                {"userId":42,"email":"admin@example.com","role":"ADMIN","roleId":1,"createdAt":1000,"sessionVersion":2}
                """);
        when(valueOperations.get("user_sessions_revoked_at:42")).thenReturn("1000");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(sessionService.isValidSession("session-a")).isFalse();
    }
}
