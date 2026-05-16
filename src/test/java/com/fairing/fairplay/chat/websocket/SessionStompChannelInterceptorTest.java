package com.fairing.fairplay.chat.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import com.fairing.fairplay.core.service.SessionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class SessionStompChannelInterceptorTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final SessionStompChannelInterceptor interceptor =
            new SessionStompChannelInterceptor(mock(SessionService.class), chatRoomRepository);

    @Test
    void rejectsUnauthenticatedAppSend() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/chat.sendMessage", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allowsAuthenticatedAppSend() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/chat.sendMessage", new StompPrincipal("1"));

        assertThatCode(() -> interceptor.preSend(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPublicQrSubscribeWithoutAuthentication() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/check-in/123", null);

        assertThatCode(() -> interceptor.preSend(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnauthenticatedChatSubscribe() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat.123", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAuthenticatedChatSubscribeForNonParticipant() {
        when(chatRoomRepository.findById(123L)).thenReturn(Optional.of(chatRoom(10L, 20L)));
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat.123", new StompPrincipal("30"));

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allowsAuthenticatedChatSubscribeForParticipant() {
        when(chatRoomRepository.findById(123L)).thenReturn(Optional.of(chatRoom(10L, 20L)));
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat.123", new StompPrincipal("10"));

        assertThatCode(() -> interceptor.preSend(message, null))
                .doesNotThrowAnyException();
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, StompPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private ChatRoom chatRoom(Long userId, Long targetId) {
        return ChatRoom.builder()
                .chatRoomId(123L)
                .userId(userId)
                .targetId(targetId)
                .build();
    }
}
