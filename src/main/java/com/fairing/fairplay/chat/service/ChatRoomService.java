package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public Optional<ChatRoom> getChatRoom(Long userId, TargetType targetType, Long targetId, Long eventId) {
        if (eventId == null) {
            // 전체관리자 문의
            return chatRoomRepository.findByUserIdAndTargetTypeAndTargetIdAndEventIdIsNull(
                    userId, targetType, targetId
            );
        }
        // 행사/부스/기타 문의
        return chatRoomRepository.findByUserIdAndTargetTypeAndTargetIdAndEventId(
                userId, targetType, targetId, eventId
        );
    }

    public ChatRoom getOrCreateRoom(Long userId, TargetType targetType, Long targetId, Long eventId) {
        return getChatRoom(userId, targetType, targetId, eventId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .userId(userId)
                        .targetType(targetType)
                        .targetId(targetId)
                        .eventId(eventId)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    public List<ChatRoom> getRoomsByUser(Long userId) {
        return chatRoomRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ChatRoom> getRoomsByManager(TargetType targetType, Long targetId) {
        return chatRoomRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }

    // ADMIN이 모든 ADMIN 타입 채팅방을 볼 수 있도록 하는 메서드
    public List<ChatRoom> getAllAdminRooms() {
        return chatRoomRepository.findByTargetTypeOrderByCreatedAtDesc(TargetType.ADMIN);
    }
}
