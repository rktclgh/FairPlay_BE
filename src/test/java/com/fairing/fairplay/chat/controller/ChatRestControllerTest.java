package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatRoomResponseDto;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.service.ChatEventHelperService;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRestControllerTest {

    private final ChatRoomService chatRoomService = mock(ChatRoomService.class);
    private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
    private final ChatEventHelperService chatEventHelperService = mock(ChatEventHelperService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ChatRestController controller = new ChatRestController(
            chatRoomService,
            chatMessageService,
            chatEventHelperService,
            userRepository);

    @Test
    void adminRoomListIncludesAdminInquiryWhenAdminIsNotStoredTargetUser() {
        CustomUserDetails admin = CustomUserDetails.fromSession(10L, "admin@example.com", "ADMIN", 1);
        ChatRoom adminInquiryRoom = ChatRoom.builder()
                .chatRoomId(45L)
                .userId(1092L)
                .targetType(TargetType.ADMIN)
                .targetId(1L)
                .createdAt(LocalDateTime.of(2026, 5, 27, 16, 37))
                .build();

        when(chatRoomService.getRoomsByUser(10L)).thenReturn(List.of());
        when(chatRoomService.getAllAdminRooms()).thenReturn(List.of(adminInquiryRoom));
        when(chatMessageService.countUnreadMessages(adminInquiryRoom, 10L, "ADMIN")).thenReturn(3L);
        when(userRepository.findAllById(java.util.Set.of(1092L))).thenReturn(List.of(Users.builder()
                .userId(1092L)
                .name("문의자")
                .build()));

        List<ChatRoomResponseDto> rooms = controller.getMyChatRooms(admin);

        assertThat(rooms).hasSize(1);
        ChatRoomResponseDto room = rooms.get(0);
        assertThat(room.getChatRoomId()).isEqualTo(45L);
        assertThat(room.getTargetType()).isEqualTo("ADMIN");
        assertThat(room.getTargetId()).isEqualTo(1L);
        assertThat(room.getUserName()).isEqualTo("문의자");
        assertThat(room.getUnreadCount()).isEqualTo(3L);
        verify(chatMessageService).countUnreadMessages(adminInquiryRoom, 10L, "ADMIN");
        verify(userRepository, never()).findById(1092L);
    }
}
