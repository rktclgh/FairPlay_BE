package com.fairing.fairplay.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatCacheServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    Cursor<String> cursor;

    @Test
    void invalidatesRoomUnreadCachesWithScanInsteadOfKeys() {
        ChatCacheService chatCacheService = new ChatCacheService(redisTemplate, new ObjectMapper());

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next())
                .thenReturn("chat:unread:10:100")
                .thenReturn("chat:unread:10:200");
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.<Collection<String>>any())).thenReturn(2L);

        chatCacheService.invalidateUnreadCachesForRoom(10L);

        ArgumentCaptor<ScanOptions> scanOptionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate).scan(scanOptionsCaptor.capture());
        assertThat(scanOptionsCaptor.getValue().getPattern()).isEqualTo("chat:unread:10:*");

        verify(redisTemplate, never()).keys(anyString());
        verify(redisTemplate).delete(org.mockito.ArgumentMatchers.<Collection<String>>any());
    }
}
