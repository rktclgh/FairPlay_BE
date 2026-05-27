package com.fairing.fairplay.chat.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import com.fairing.fairplay.chat.service.ChatRoomAccessService;
import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class SessionStompChannelInterceptorTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ChatRoomAccessService chatRoomAccessService = new ChatRoomAccessService(userRepository);
    private final SessionStompChannelInterceptor interceptor =
            new SessionStompChannelInterceptor(mock(SessionService.class), chatRoomRepository, chatRoomAccessService);

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
    void rejectsUnauthenticatedQrSubscribe() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/check-in/123", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
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

    @Test
    void allowsAdminSubscribeToAdminInquiryWhenNotStoredTarget() {
        when(chatRoomRepository.findById(123L)).thenReturn(Optional.of(adminInquiryRoom(1092L, 1L)));
        when(userRepository.findByUserIdInAndRoleCode_Code(java.util.Set.of(10L), "ADMIN"))
                .thenReturn(java.util.List.of(mock(com.fairing.fairplay.user.entity.Users.class)));
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

    private ChatRoom adminInquiryRoom(Long userId, Long targetId) {
        return ChatRoom.builder()
                .chatRoomId(123L)
                .userId(userId)
                .targetType(TargetType.ADMIN)
                .targetId(targetId)
                .build();
    }
}
