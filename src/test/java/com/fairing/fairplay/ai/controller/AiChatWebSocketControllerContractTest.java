package com.fairing.fairplay.ai.controller;

import com.fairing.fairplay.ai.dto.AiChatMessageDto;
import com.fairing.fairplay.ai.service.ChatAiService;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatWebSocketControllerContractTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    ChatAiService chatAiService;

    @Mock
    ChatMessageService chatMessageService;

    @InjectMocks
    AiChatWebSocketController controller;

    @Test
    void sendAiMessagePersistsExactlyOneMessageForOneUserMessage() {
        Long roomId = 101L;
        Long senderId = 42L;
        when(chatMessageService.sendMessage(roomId, senderId, "예매 방법 알려줘"))
                .thenReturn(savedUserMessage(roomId, senderId, "예매 방법 알려줘"));

        controller.sendAiMessage(aiChatMessage(roomId, senderId), principal(senderId));

        verify(chatMessageService).sendMessage(roomId, senderId, "예매 방법 알려줘");
    }

    @Test
    void sendAiMessageDoesNotCallChatAiServiceDirectly() {
        Long roomId = 101L;
        Long senderId = 42L;
        when(chatMessageService.sendMessage(roomId, senderId, "예매 방법 알려줘"))
                .thenReturn(savedUserMessage(roomId, senderId, "예매 방법 알려줘"));

        controller.sendAiMessage(aiChatMessage(roomId, senderId), principal(senderId));

        verifyNoInteractions(chatAiService);
    }

    @Test
    void sendAiMessageBroadcastsSavedUserMessageDtoWithChatMessageIdAndSentAt() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatMessageResponseDto savedUserMessage = savedUserMessage(roomId, senderId, "저장된 사용자 질문");
        when(chatMessageService.sendMessage(roomId, senderId, "예매 방법 알려줘"))
                .thenReturn(savedUserMessage);

        controller.sendAiMessage(aiChatMessage(roomId, senderId), principal(senderId));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/ai-chat." + roomId),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue())
                .isInstanceOf(ChatMessageResponseDto.class)
                .isSameAs(savedUserMessage)
                .extracting("chatMessageId", "sentAt")
                .containsExactly(501L, LocalDateTime.of(2026, 5, 18, 10, 0));
    }

    @Test
    void sendAiMessageBroadcastsSavedUserMessageWithoutSystemError() {
        Long roomId = 101L;
        Long senderId = 42L;
        when(chatMessageService.sendMessage(roomId, senderId, "예매 방법 알려줘"))
                .thenReturn(savedUserMessage(roomId, senderId, "저장된 사용자 질문"));

        controller.sendAiMessage(aiChatMessage(roomId, senderId), principal(senderId));

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/ai-chat." + roomId),
                (Object) argThat(payload -> payload instanceof AiChatMessageDto dto
                        && "system_error".equals(dto.getType()))
        );
        verifyNoInteractions(chatAiService);
    }

    private AiChatMessageDto aiChatMessage(Long roomId, Long senderId) {
        return AiChatMessageDto.builder()
                .chatRoomId(roomId)
                .senderId(senderId)
                .content("예매 방법 알려줘")
                .build();
    }

    private Principal principal(Long senderId) {
        return () -> senderId.toString();
    }

    private ChatMessageResponseDto savedUserMessage(Long roomId, Long senderId, String content) {
        return ChatMessageResponseDto.builder()
                .chatMessageId(501L)
                .chatRoomId(roomId)
                .senderId(senderId)
                .content(content)
                .sentAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .isRead(false)
                .build();
    }
}
