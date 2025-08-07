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
        System.out.println("=== WebSocket 메시지 수신 ===");
        System.out.println("Principal: " + principal);
        System.out.println("Message: " + message);
        System.out.println("ChatRoomId: " + message.getChatRoomId());
        System.out.println("Content: " + message.getContent());
        System.out.println("SenderId from message: " + message.getSenderId());
        
        // Principal이 null이면 메시지에서 senderId 사용
        Long senderId;
        if (principal != null) {
            senderId = Long.parseLong(principal.getName());
            System.out.println("Using senderId from Principal: " + senderId);
        } else if (message.getSenderId() != null) {
            senderId = message.getSenderId();
            System.out.println("Using senderId from message: " + senderId);
        } else {
            // 테스트용 기본값
            senderId = 1L;
            System.out.println("Using default senderId: " + senderId);
        }
        
        System.out.println("Final senderId: " + senderId);
        
        ChatMessageResponseDto response = chatMessageService.sendMessage(message.getChatRoomId(), senderId, message.getContent());
        System.out.println("Created response: " + response);
        
        // 채팅방 내 메시지 브로드캐스팅
        String topic = "/topic/chat." + message.getChatRoomId();
        System.out.println("Broadcasting to topic: " + topic);
        messagingTemplate.convertAndSend(topic, response);
        
        // 채팅방 목록 업데이트 알림 브로드캐스팅 (모든 사용자에게)
        String roomListTopic = "/topic/chat-room-list";
        messagingTemplate.convertAndSend(roomListTopic, "UPDATE");
        System.out.println("채팅방 목록 업데이트 알림 전송");
        
        System.out.println("메시지 브로드캐스팅 완료");
        System.out.println("===============================");
    }

}
