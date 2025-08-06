package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.dto.ChatRoomResponseDto;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // [유저/관리자] 내 채팅방 리스트
    @GetMapping("/rooms")
    public List<ChatRoomResponseDto> getMyChatRooms(@RequestParam Long userId) {
        // 프론트에서 userId or 관리자면 targetType/targetId로 요청
        return chatRoomService.getRoomsByUser(userId)
                .stream()
                .map(room -> ChatRoomResponseDto.builder()
                        .chatRoomId(room.getChatRoomId())
                        .eventId(room.getEventId())
                        .userId(room.getUserId())
                        .targetType(room.getTargetType().name())
                        .targetId(room.getTargetId())
                        .createdAt(room.getCreatedAt())
                        .closedAt(room.getClosedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // [관리자] 내가 관리하는 채팅방 리스트
    @GetMapping("/rooms/manager")
    public List<ChatRoomResponseDto> getChatRoomsByManager(@RequestParam String targetType,
                                                           @RequestParam Long targetId) {
        TargetType tType = TargetType.valueOf(targetType);
        return chatRoomService.getRoomsByManager(tType, targetId)
                .stream()
                .map(room -> ChatRoomResponseDto.builder()
                        .chatRoomId(room.getChatRoomId())
                        .eventId(room.getEventId())
                        .userId(room.getUserId())
                        .targetType(room.getTargetType().name())
                        .targetId(room.getTargetId())
                        .createdAt(room.getCreatedAt())
                        .closedAt(room.getClosedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 채팅방 생성/입장(없으면 생성, 있으면 반환)
    @PostMapping("/room")
    public ChatRoomResponseDto createOrEnterRoom(@RequestParam Long userId,
                                                 @RequestParam String targetType,
                                                 @RequestParam Long targetId,
                                                 @RequestParam(required = false) Long eventId) {
        TargetType tType = TargetType.valueOf(targetType);
        ChatRoom room = chatRoomService.getOrCreateRoom(userId, tType, targetId, eventId);
        return ChatRoomResponseDto.builder()
                .chatRoomId(room.getChatRoomId())
                .eventId(room.getEventId())
                .userId(room.getUserId())
                .targetType(room.getTargetType().name())
                .targetId(room.getTargetId())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .build();
    }

    // 채팅방 내 메시지 전체(시간순)
    @GetMapping("/messages")
    public List<ChatMessageResponseDto> getMessages(@RequestParam Long chatRoomId) {
        return chatMessageService.getMessages(chatRoomId);
    }

    // 메시지 전송
    @PostMapping("/message")
    public ChatMessageResponseDto sendMessage(@RequestBody ChatMessageRequestDto dto,
                                              @RequestParam Long senderId) {
        return chatMessageService.sendMessage(dto.getChatRoomId(), senderId, dto.getContent());
    }

    // 안읽은 메시지 개수
    @GetMapping("/unread-count")
    public Long countUnreadMessages(@RequestParam Long chatRoomId, @RequestParam Long myUserId) {
        return chatMessageService.countUnreadMessages(chatRoomId, myUserId);
    }
}
