package com.fairing.fairplay.chat.repository;

import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 채팅방 내 전체 메시지(시간순)
    List<ChatMessage> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);

    // 페이징된 메시지 조회 (최신 메시지가 먼저 - 역순)
    Page<ChatMessage> findByChatRoomOrderBySentAtDesc(ChatRoom chatRoom, Pageable pageable);
    
    // 특정 메시지 이전의 메시지들 조회 (무한스크롤용)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom AND cm.chatMessageId < :lastMessageId ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findByChatRoomAndMessageIdLessThanOrderBySentAtDesc(
            @Param("chatRoom") ChatRoom chatRoom, 
            @Param("lastMessageId") Long lastMessageId, 
            Pageable pageable
    );

    // 안 읽은 메시지 개수 (특정 방, 특정 유저 기준)
    Long countByChatRoomAndIsReadFalseAndSenderIdNot(ChatRoom chatRoom, Long senderId);

    // 읽지 않은 메시지 목록 (내가 보낸 게 아닌)
    List<ChatMessage> findByChatRoomAndIsReadFalseAndSenderIdNot(ChatRoom chatRoom, Long senderId);
}
