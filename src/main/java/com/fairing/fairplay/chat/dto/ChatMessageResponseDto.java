package com.fairing.fairplay.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponseDto {
    private Long chatMessageId;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isRead;
    
    @Override
    public String toString() {
        return "ChatMessageResponseDto{" +
                "chatMessageId=" + chatMessageId +
                ", chatRoomId=" + chatRoomId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", sentAt=" + sentAt +
                ", isRead=" + isRead +
                '}';
    }
}
