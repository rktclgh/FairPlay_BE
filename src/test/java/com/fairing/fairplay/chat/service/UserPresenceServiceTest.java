package com.fairing.fairplay.chat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPresenceServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    SetOperations<String, String> setOperations;

    @Mock
    Cursor<String> cursor;

    @Test
    void cleanupExpiredPresenceScansOnlineUsersAndRemovesOnlyStaleOrInvalidEntries() {
        UserPresenceService userPresenceService = new UserPresenceService(redisTemplate);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.scan(any(String.class), any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next())
                .thenReturn("10")
                .thenReturn("invalid")
                .thenReturn("20");
        when(redisTemplate.hasKey("user:online:10")).thenReturn(true);
        when(redisTemplate.hasKey("user:online:20")).thenReturn(false);

        userPresenceService.cleanupExpiredPresence();

        verify(setOperations).scan(any(String.class), any(ScanOptions.class));
        verify(setOperations, never()).members("users:online");
        verify(setOperations, never()).remove("users:online", "10");
        verify(setOperations).remove("users:online", "invalid");
        verify(setOperations).remove("users:online", "20");
    }
}
