package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

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

        return ChatMessageResponseDto.builder()
                .chatMessageId(saved.getChatMessageId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .sentAt(saved.getSentAt())
                .isRead(saved.getIsRead())
                .build();
    }

    public List<ChatMessageResponseDto> getMessages(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        return chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)
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
    }

    @Transactional
    public void markAsRead(Long chatMessageId) {
        ChatMessage message = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        message.setIsRead(true);
        chatMessageRepository.save(message);
    }

    public Long countUnreadMessages(Long chatRoomId, Long myUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        return chatMessageRepository.countByChatRoomAndIsReadFalseAndSenderIdNot(chatRoom, myUserId);
    }
}
