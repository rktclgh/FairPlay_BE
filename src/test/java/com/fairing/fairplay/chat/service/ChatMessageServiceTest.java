package com.fairing.fairplay.chat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

class ChatMessageServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ChatCacheService chatCacheService = mock(ChatCacheService.class);
    private final ChatMessageService chatMessageService = new ChatMessageService(
            chatMessageRepository,
            chatRoomRepository,
            eventPublisher,
            chatCacheService,
            Runnable::run);

    @Test
    void rejectsMessageReadForNonParticipant() {
        when(chatRoomRepository.findById(123L)).thenReturn(Optional.of(chatRoom(10L, 20L)));
        when(chatCacheService.getCachedMessages(123L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> chatMessageService.getMessages(123L, 30L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMessageWriteForNonParticipant() {
        when(chatRoomRepository.findById(123L)).thenReturn(Optional.of(chatRoom(10L, 20L)));

        assertThatThrownBy(() -> chatMessageService.sendMessage(123L, 30L, "hello"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ChatRoom chatRoom(Long userId, Long targetId) {
        return ChatRoom.builder()
                .chatRoomId(123L)
                .userId(userId)
                .targetId(targetId)
                .build();
    }
}
