// src/main/java/com/fairing/fairplay/chat/event/ChatMessageCreatedEvent.java
package com.fairing.fairplay.chat.event;

import lombok.Getter;

@Getter
public class ChatMessageCreatedEvent {
    private final Long chatRoomId;
    private final Long senderId;
    private final Long chatMessageId;
    private final String content;

    public ChatMessageCreatedEvent(Long chatRoomId, Long senderId) {
        this(chatRoomId, senderId, null, null);
    }

    public ChatMessageCreatedEvent(Long chatRoomId, Long senderId, Long chatMessageId, String content) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.chatMessageId = chatMessageId;
        this.content = content;
    }
}
