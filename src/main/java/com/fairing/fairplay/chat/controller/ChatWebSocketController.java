package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatPresenceService chatPresenceService;

    // STOMP 구독: /topic/chat.{chatRoomId}
    // 전송: /app/chat.sendMessage

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequestDto message, Principal principal) {
        // principal.getName()에 userId or username이 들어있어야 함!
        Long senderId = Long.parseLong(principal.getName());
        ChatMessageResponseDto response = chatMessageService.sendMessage(message.getChatRoomId(), senderId, message.getContent());
        messagingTemplate.convertAndSend("/topic/chat." + message.getChatRoomId(), response);
    }

    @MessageMapping("/chat.enter")
    public void enter(Principal principal, @Header("isManager") Boolean isManager) {
        Long userId = Long.parseLong(principal.getName());
        chatPresenceService.setOnline(isManager, userId);
    }

    @MessageMapping("/chat.leave")
    public void leave(Principal principal, @Header("isManager") Boolean isManager) {
        Long userId = Long.parseLong(principal.getName());
        chatPresenceService.setOffline(isManager, userId);
    }
}
