package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.event.ChatMessageCreatedEvent;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceEventContractTest {

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    ChatCacheService chatCacheService;

    @Mock
    Executor chatCacheExecutor;

    @InjectMocks
    ChatMessageService chatMessageService;

    @Test
    void publishesSavedUserMessageIdentityAndContentInCreatedEvent() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomId(roomId)
                .userId(senderId)
                .targetType(TargetType.AI)
                .targetId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage savedMessage = ChatMessage.builder()
                .chatMessageId(501L)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .content("저장된 사용자 질문")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
        ArgumentCaptor<ChatMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        chatMessageService.sendMessage(roomId, senderId, "요청 사용자 질문");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ChatMessageCreatedEvent event = eventCaptor.getValue();
        assertEquals(roomId, event.getChatRoomId());
        assertEquals(senderId, event.getSenderId());
        assertEquals(501L, event.getChatMessageId());
        assertEquals("저장된 사용자 질문", event.getContent());
    }

    @Test
    void defersMessageCacheWorkUntilAfterCommitAndExecutorDispatch() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomId(roomId)
                .userId(senderId)
                .targetType(TargetType.AI)
                .targetId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage savedMessage = ChatMessage.builder()
                .chatMessageId(501L)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .content("저장된 사용자 질문")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
        ArgumentCaptor<Runnable> cacheTaskCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        TransactionSynchronizationManager.initSynchronization();
        try {
            chatMessageService.sendMessage(roomId, senderId, "요청 사용자 질문");

            verifyNoInteractions(chatCacheService);
            verify(chatCacheExecutor, never()).execute(any(Runnable.class));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(chatCacheExecutor).execute(cacheTaskCaptor.capture());
        cacheTaskCaptor.getValue().run();

        verify(chatCacheService).cacheMessage(any(Long.class), any());
        verify(chatCacheService).invalidateUnreadCachesForRoom(roomId);
    }

    @Test
    void defersReadCacheInvalidationUntilAfterCommitAndExecutorDispatch() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomId(roomId)
                .userId(senderId)
                .targetType(TargetType.AI)
                .targetId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage message = ChatMessage.builder()
                .chatMessageId(501L)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .content("읽음 처리할 메시지")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
        ArgumentCaptor<Runnable> cacheTaskCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(chatMessageRepository.findById(501L)).thenReturn(Optional.of(message));

        TransactionSynchronizationManager.initSynchronization();
        try {
            chatMessageService.markAsRead(501L);

            verifyNoInteractions(chatCacheService);
            verify(chatCacheExecutor, never()).execute(any(Runnable.class));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(chatCacheExecutor).execute(cacheTaskCaptor.capture());
        cacheTaskCaptor.getValue().run();

        verify(chatCacheService).invalidateUnreadCachesForRoom(roomId);
    }
}
