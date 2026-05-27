package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.dto.ChatMessagePageResponseDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.event.ChatMessageCreatedEvent;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatCacheService chatCacheService;
    private final Executor chatCacheExecutor;
    private final ChatRoomAccessService chatRoomAccessService;

    public ChatMessageService(
            ChatMessageRepository chatMessageRepository,
            ChatRoomRepository chatRoomRepository,
            ApplicationEventPublisher eventPublisher,
            ChatCacheService chatCacheService,
            @Qualifier("chatCacheTaskExecutor") Executor chatCacheExecutor,
            ChatRoomAccessService chatRoomAccessService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.eventPublisher = eventPublisher;
        this.chatCacheService = chatCacheService;
        this.chatCacheExecutor = chatCacheExecutor;
        this.chatRoomAccessService = chatRoomAccessService;
    }

    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, String content) {
        return sendMessage(chatRoomId, senderId, null, content);
    }

    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, String roleCode, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        chatRoomAccessService.assertCanAccess(chatRoom, senderId, roleCode, "채팅방에 참여하지 않은 사용자는 메시지를 보낼 수 없습니다.");

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

        cacheMessageAfterCommit(chatRoomId, responseDto);

        // 이벤트 발행
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(
                chatRoomId,
                senderId,
                saved.getChatMessageId(),
                saved.getContent()
        ));

        return responseDto;
    }

    public List<ChatMessageResponseDto> getMessages(Long chatRoomId, Long viewerId) {
        return getMessages(chatRoomId, viewerId, null);
    }

    public List<ChatMessageResponseDto> getMessages(Long chatRoomId, Long viewerId, String roleCode) {
        // 먼저 Redis 캐시에서 확인
        List<ChatMessageResponseDto> cachedMessages = chatCacheService.getCachedMessages(chatRoomId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        chatRoomAccessService.assertCanAccess(chatRoom, viewerId, roleCode, "채팅방에 참여하지 않은 사용자는 메시지를 읽을 수 없습니다.");

        if (!cachedMessages.isEmpty()) {
            System.out.println("Redis 캐시에서 메시지 조회: roomId=" + chatRoomId + ", count=" + cachedMessages.size());
            return cachedMessages;
        }
        
        // 캐시가 없으면 DB에서 조회하고 캐싱
        System.out.println("DB에서 메시지 조회: roomId=" + chatRoomId);
        
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

    /**
     * 페이징된 메시지 조회 (최신 메시지부터, 20개 단위)
     * @param chatRoomId 채팅방 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (기본 20)
     * @return 페이징된 메시지 응답
     */
    public ChatMessagePageResponseDto getMessagesPaged(Long chatRoomId, Long viewerId, int page, int size) {
        return getMessagesPaged(chatRoomId, viewerId, null, page, size);
    }

    public ChatMessagePageResponseDto getMessagesPaged(Long chatRoomId, Long viewerId, String roleCode, int page, int size) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        chatRoomAccessService.assertCanAccess(chatRoom, viewerId, roleCode, "채팅방에 참여하지 않은 사용자는 메시지를 읽을 수 없습니다.");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomOrderBySentAtDesc(chatRoom, pageable);
        
        // 메시지를 시간순으로 정렬 (최신 메시지가 아래로)
        List<ChatMessageResponseDto> messages = messagePage.getContent().stream()
                .map(msg -> ChatMessageResponseDto.builder()
                        .chatMessageId(msg.getChatMessageId())
                        .chatRoomId(chatRoomId)
                        .senderId(msg.getSenderId())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .isRead(msg.getIsRead())
                        .build())
                .collect(Collectors.toList());
        
        // 시간순으로 다시 정렬 (채팅창에서는 오래된 메시지가 위로)
        Collections.reverse(messages);
        
        // 다음 페이지를 위한 커서 (현재 페이지의 가장 오래된 메시지 ID)
        Long nextCursor = messages.isEmpty() ? null : 
                messages.get(0).getChatMessageId();
        
        return ChatMessagePageResponseDto.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasNext(messagePage.hasNext())
                .currentPage(page)
                .pageSize(size)
                .totalElements(messagePage.getTotalElements())
                .build();
    }

    /**
     * 커서 기반 무한스크롤 메시지 조회
     * @param chatRoomId 채팅방 ID
     * @param lastMessageId 마지막으로 로드된 메시지 ID (null이면 최신부터)
     * @param size 가져올 메시지 수 (기본 20)
     * @return 페이징된 메시지 응답
     */
    public ChatMessagePageResponseDto getMessagesWithCursor(Long chatRoomId, Long viewerId, Long lastMessageId, int size) {
        return getMessagesWithCursor(chatRoomId, viewerId, null, lastMessageId, size);
    }

    public ChatMessagePageResponseDto getMessagesWithCursor(Long chatRoomId, Long viewerId, String roleCode, Long lastMessageId, int size) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        chatRoomAccessService.assertCanAccess(chatRoom, viewerId, roleCode, "채팅방에 참여하지 않은 사용자는 메시지를 읽을 수 없습니다.");
        
        Pageable pageable = PageRequest.of(0, size);
        Page<ChatMessage> messagePage;
        
        if (lastMessageId == null) {
            // 첫 번째 로드: 최신 메시지부터
            messagePage = chatMessageRepository.findByChatRoomOrderBySentAtDesc(chatRoom, pageable);
        } else {
            // 다음 페이지: lastMessageId보다 이전 메시지들
            messagePage = chatMessageRepository.findByChatRoomAndMessageIdLessThanOrderBySentAtDesc(
                    chatRoom, lastMessageId, pageable);
        }
        
        List<ChatMessageResponseDto> messages = messagePage.getContent().stream()
                .map(msg -> ChatMessageResponseDto.builder()
                        .chatMessageId(msg.getChatMessageId())
                        .chatRoomId(chatRoomId)
                        .senderId(msg.getSenderId())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .isRead(msg.getIsRead())
                        .build())
                .collect(Collectors.toList());
        
        // 시간순으로 정렬 (채팅창에서는 오래된 메시지가 위로)
        Collections.reverse(messages);
        
        // 다음 페이지를 위한 커서 (현재 페이지의 가장 오래된 메시지 ID)
        Long nextCursor = messages.isEmpty() ? null : 
                messages.get(0).getChatMessageId();
        
        return ChatMessagePageResponseDto.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasNext(messagePage.hasNext())
                .currentPage(0) // 커서 기반에서는 의미없음
                .pageSize(size)
                .totalElements(messagePage.getTotalElements())
                .build();
    }

    @Transactional
    public void markAsRead(Long chatMessageId) {
        ChatMessage message = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        message.setIsRead(true);
        chatMessageRepository.save(message);
        
        // 읽음 처리 후 해당 채팅방의 캐시 무효화
        Long chatRoomId = message.getChatRoom().getChatRoomId();
        invalidateUnreadCachesAfterCommit(chatRoomId);
    }

    public Long countUnreadMessages(Long chatRoomId, Long myUserId) {
        return countUnreadMessages(chatRoomId, myUserId, null);
    }

    public Long countUnreadMessages(Long chatRoomId, Long myUserId, String roleCode) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        return countUnreadMessages(chatRoom, myUserId, roleCode);
    }

    public Long countUnreadMessages(ChatRoom chatRoom, Long myUserId, String roleCode) {
        chatRoomAccessService.assertCanAccess(chatRoom, myUserId, roleCode, "채팅방에 참여하지 않은 사용자는 읽지 않은 메시지 수를 볼 수 없습니다.");
        Long chatRoomId = chatRoom.getChatRoomId();

        // 먼저 Redis 캐시에서 확인
        Long cachedCount = chatCacheService.getCachedUnreadCount(chatRoomId, myUserId);
        if (cachedCount != null) {
            return cachedCount;
        }

        Long count = chatMessageRepository.countByChatRoomAndIsReadFalseAndSenderIdNot(chatRoom, myUserId);
        
        // 결과를 Redis에 캐싱
        chatCacheService.cacheUnreadCount(chatRoomId, myUserId, count);
        
        return count;
    }

    @Transactional
    public void markRoomMessagesAsRead(Long chatRoomId, Long myUserId) {
        markRoomMessagesAsRead(chatRoomId, myUserId, null);
    }

    @Transactional
    public void markRoomMessagesAsRead(Long chatRoomId, Long myUserId, String roleCode) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        chatRoomAccessService.assertCanAccess(chatRoom, myUserId, roleCode, "채팅방에 참여하지 않은 사용자는 메시지를 읽음 처리할 수 없습니다.");
        
        // 내가 보낸 메시지가 아닌 읽지 않은 메시지들을 읽음으로 처리
        List<ChatMessage> unreadMessages = chatMessageRepository.findByChatRoomAndIsReadFalseAndSenderIdNot(chatRoom, myUserId);
        for (ChatMessage message : unreadMessages) {
            message.setIsRead(true);
        }
        chatMessageRepository.saveAll(unreadMessages);
        
        // 읽음 처리 후 해당 채팅방의 모든 사용자 캐시 무효화
        invalidateUnreadCachesAfterCommit(chatRoomId);
        
        System.out.println("채팅방 " + chatRoomId + "의 " + unreadMessages.size() + "개 메시지를 읽음 처리하고 캐시 무효화");
    }
    
    /**
     * 특정 채팅방의 모든 사용자에 대한 읽지 않은 메시지 캐시 무효화
     */
    private void cacheMessageAfterCommit(Long chatRoomId, ChatMessageResponseDto responseDto) {
        runCacheTaskAfterCommit("message cache refresh", () -> {
            chatCacheService.cacheMessage(chatRoomId, responseDto);
            invalidateUnreadCaches(chatRoomId);
        });
    }

    private void invalidateUnreadCachesAfterCommit(Long chatRoomId) {
        runCacheTaskAfterCommit("unread cache invalidation", () -> invalidateUnreadCaches(chatRoomId));
    }

    private void runCacheTaskAfterCommit(String operation, Runnable task) {
        Runnable asyncTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("채팅 {} 비동기 처리 실패", operation, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitCacheTask(operation, asyncTask);
                }
            });
            return;
        }

        submitCacheTask(operation, asyncTask);
    }

    private void submitCacheTask(String operation, Runnable asyncTask) {
        try {
            chatCacheExecutor.execute(asyncTask);
        } catch (RuntimeException e) {
            log.warn("채팅 {} 작업 제출 실패", operation, e);
        }
    }

    private void invalidateUnreadCaches(Long chatRoomId) {
        // 해당 채팅방의 모든 사용자 캐시를 무효화하기 위해 
        // 채팅방 참가자들의 캐시를 무효화해야 하지만, 
        // 단순화를 위해 ChatCacheService에 패턴 기반 삭제 메서드 추가
        chatCacheService.invalidateUnreadCachesForRoom(chatRoomId);
    }

}
