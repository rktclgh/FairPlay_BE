package com.fairing.fairplay.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDto {
    private Long chatRoomId;
    private String content;
    private Long senderId;
    
    @Override
    public String toString() {
        return "ChatMessageRequestDto{" +
                "chatRoomId=" + chatRoomId +
                ", content='" + content + '\'' +
                ", senderId=" + senderId +
                '}';
    }
}
