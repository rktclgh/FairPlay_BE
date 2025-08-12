package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fairing.fairplay.chat.event.ChatMessageCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatCacheService chatCacheService;

    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderId(senderId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // DTO 생성
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .chatMessageId(saved.getChatMessageId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .sentAt(saved.getSentAt())
                .isRead(saved.getIsRead())
                .build();

        // Redis에 즉시 캐싱
        chatCacheService.cacheMessage(chatRoomId, responseDto);
        
        // 새 메시지로 인해 읽지 않은 메시지 수가 변경되므로 캐시 무효화
        invalidateUnreadCaches(chatRoomId);

        // 이벤트 발행
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(chatRoomId, senderId));

        return responseDto;
    }

    public List<ChatMessageResponseDto> getMessages(Long chatRoomId) {
        // 먼저 Redis 캐시에서 확인
        List<ChatMessageResponseDto> cachedMessages = chatCacheService.getCachedMessages(chatRoomId);
        
        if (!cachedMessages.isEmpty()) {
            System.out.println("Redis 캐시에서 메시지 조회: roomId=" + chatRoomId + ", count=" + cachedMessages.size());
            return cachedMessages;
        }
        
        // 캐시가 없으면 DB에서 조회하고 캐싱
        System.out.println("DB에서 메시지 조회: roomId=" + chatRoomId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        List<ChatMessageResponseDto> messages = chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)
                .stream()
                .map(msg -> ChatMessageResponseDto.builder()
                        .chatMessageId(msg.getChatMessageId())
                        .chatRoomId(chatRoomId)
                        .senderId(msg.getSenderId())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .isRead(msg.getIsRead())
                        .build())
                .collect(Collectors.toList());
        
        // 조회한 메시지들을 Redis에 캐싱
        for (ChatMessageResponseDto message : messages) {
            chatCacheService.cacheMessage(chatRoomId, message);
        }
        
        return messages;
    }

    @Transactional
    public void markAsRead(Long chatMessageId) {
        ChatMessage message = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        message.setIsRead(true);
        chatMessageRepository.save(message);
        
        // 읽음 처리 후 해당 채팅방의 캐시 무효화
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        invalidateUnreadCaches(chatRoomId);
    }

    public Long countUnreadMessages(Long chatRoomId, Long myUserId) {
        // 먼저 Redis 캐시에서 확인
        Long cachedCount = chatCacheService.getCachedUnreadCount(chatRoomId, myUserId);
        if (cachedCount != null) {
            return cachedCount;
        }
        
        // 캐시가 없으면 DB에서 조회하고 캐싱
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        Long count = chatMessageRepository.countByChatRoomAndIsReadFalseAndSenderIdNot(chatRoom, myUserId);
        
        // 결과를 Redis에 캐싱
        chatCacheService.cacheUnreadCount(chatRoomId, myUserId, count);
        
        return count;
    }

    @Transactional
    public void markRoomMessagesAsRead(Long chatRoomId, Long myUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        // 내가 보낸 메시지가 아닌 읽지 않은 메시지들을 읽음으로 처리
        List<ChatMessage> unreadMessages = chatMessageRepository.findByChatRoomAndIsReadFalseAndSenderIdNot(chatRoom, myUserId);
        for (ChatMessage message : unreadMessages) {
            message.setIsRead(true);
        }
        chatMessageRepository.saveAll(unreadMessages);
        
        // 읽음 처리 후 해당 채팅방의 모든 사용자 캐시 무효화
        invalidateUnreadCaches(chatRoomId);
        
        System.out.println("채팅방 " + chatRoomId + "의 " + unreadMessages.size() + "개 메시지를 읽음 처리하고 캐시 무효화");
    }
    
    /**
     * 특정 채팅방의 모든 사용자에 대한 읽지 않은 메시지 캐시 무효화
     */
    private void invalidateUnreadCaches(Long chatRoomId) {
        // 해당 채팅방의 모든 사용자 캐시를 무효화하기 위해 
        // 채팅방 참가자들의 캐시를 무효화해야 하지만, 
        // 단순화를 위해 ChatCacheService에 패턴 기반 삭제 메서드 추가
        chatCacheService.invalidateUnreadCachesForRoom(chatRoomId);
    }

}
