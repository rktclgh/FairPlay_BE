// src/main/java/com/fairing/fairplay/chat/event/ChatMessageCreatedEvent.java
package com.fairing.fairplay.chat.event;

import lombok.Getter;

@Getter
public class ChatMessageCreatedEvent {
    private final Long chatRoomId;
    private final Long senderId;

    public ChatMessageCreatedEvent(Long chatRoomId, Long senderId) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
    }
}
