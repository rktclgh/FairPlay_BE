package com.fairing.fairplay.chat.repository;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 전체관리자 문의 (eventId = null) → targetType은 Enum 타입!
    Optional<ChatRoom> findByUserIdAndTargetTypeAndTargetIdAndEventIdIsNull(Long userId, TargetType targetType, Long targetId);

    // 행사/부스/기타 문의 (eventId != null) → targetType은 Enum 타입!
    Optional<ChatRoom> findByUserIdAndTargetTypeAndTargetIdAndEventId(Long userId, TargetType targetType, Long targetId, Long eventId);

    // 유저가 참여한 전체 채팅방 목록 (나의 문의내역용)
    List<ChatRoom> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 관리자/운영자가 관리하는 채팅방 리스트 (운영자/관리자 채팅 관리용)
    List<ChatRoom> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, Long targetId);
}
