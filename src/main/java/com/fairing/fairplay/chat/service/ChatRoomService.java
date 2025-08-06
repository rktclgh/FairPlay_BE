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

    // 👉 채팅방 단건 조회(전체관리자 문의까지 포함)
    public Optional<ChatRoom> getChatRoom(Long userId, TargetType targetType, Long targetId, Long eventId) {
        if (eventId == null) {
            // 전체관리자 문의 (targetType을 .name()으로 전달)
            return chatRoomRepository.findByUserIdAndTargetTypeAndTargetIdAndEventIdIsNull(
                    userId, targetType.name(), targetId
            );
        }
        // 행사/부스/기타 문의
        return chatRoomRepository.findByUserIdAndTargetTypeAndTargetIdAndEventId(
                userId, targetType, targetId, eventId
        );
    }

    // 👉 채팅방이 없으면 새로 생성
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

    // 👉 유저의 모든 문의/채팅방 리스트(최신순)
    public List<ChatRoom> getRoomsByUser(Long userId) {
        return chatRoomRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 👉 특정 관리자/운영자가 담당하는 채팅방 리스트(최신순)
    public List<ChatRoom> getRoomsByManager(TargetType targetType, Long targetId) {
        return chatRoomRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }
}
