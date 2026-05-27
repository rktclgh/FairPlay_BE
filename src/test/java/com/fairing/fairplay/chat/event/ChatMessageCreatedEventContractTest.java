package com.fairing.fairplay.chat.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMessageCreatedEventContractTest {

    @Test
    void exposesCreatedMessageIdentitySoAiResponderCanAnswerThatMessage() {
        Method messageIdGetter = assertDoesNotThrow(
                () -> ChatMessageCreatedEvent.class.getMethod("getChatMessageId"),
                "ChatMessageCreatedEvent는 같은 방의 연속 메시지를 구분할 수 있도록 생성된 메시지 ID를 노출해야 한다."
        );

        assertEquals(Long.class, messageIdGetter.getReturnType());
    }

    @Test
    void exposesCreatedMessageContentSoAiResponderDoesNotInferFromLatestRoomHistory() {
        Method contentGetter = assertDoesNotThrow(
                () -> ChatMessageCreatedEvent.class.getMethod("getContent"),
                "ChatMessageCreatedEvent는 이벤트가 가리키는 사용자 질문 content를 노출해야 한다."
        );

        assertEquals(String.class, contentGetter.getReturnType());
    }

    @Test
    void canBeConstructedWithMessageIdentityAndContent() {
        Constructor<ChatMessageCreatedEvent> constructor = assertDoesNotThrow(
                () -> ChatMessageCreatedEvent.class.getConstructor(Long.class, Long.class, Long.class, String.class),
                "ChatMessageCreatedEvent 생성 시 roomId, senderId 외에 messageId와 content도 함께 전달되어야 한다."
        );

        assertDoesNotThrow(() -> constructor.newInstance(101L, 42L, 501L, "예매 방법 알려줘"));
    }
}
