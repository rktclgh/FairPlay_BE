package com.fairing.fairplay.chat.repository;

import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 채팅방 내 전체 메시지(시간순)
    List<ChatMessage> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);

    // 안 읽은 메시지 개수 (특정 방, 특정 유저 기준)
    Long countByChatRoomAndIsReadFalseAndSenderIdNot(ChatRoom chatRoom, Long senderId);

    // 읽지 않은 메시지 목록 (내가 보낸 게 아닌)
    List<ChatMessage> findByChatRoomAndIsReadFalseAndSenderIdNot(ChatRoom chatRoom, Long senderId);
}
