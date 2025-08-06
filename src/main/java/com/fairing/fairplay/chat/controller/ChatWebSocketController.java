package com.fairing.fairplay.chat.websocket;

import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

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
    public void sendMessage(@Payload ChatMessageRequestDto message,
                            @Header("userId") Long senderId) {
        // 1. DB에 메시지 저장
        ChatMessageResponseDto response = chatMessageService.sendMessage(message.getChatRoomId(), senderId, message.getContent());
        // 2. 실시간 브로드캐스트(해당 채팅방 구독자에게)
        messagingTemplate.convertAndSend("/topic/chat." + message.getChatRoomId(), response);
    }

    // 클라이언트에서 입장 시 호출 (온라인 처리)
    @MessageMapping("/chat.enter")
    public void enter(@Header("userId") Long userId,
                      @Header("isManager") Boolean isManager) {
        chatPresenceService.setOnline(isManager, userId);
        // (옵션) 구독자에게 온라인 상태 알림
        // messagingTemplate.convertAndSend("/topic/presence." + userId, "online");
    }

    // 클라이언트에서 퇴장 시 호출 (오프라인 처리)
    @MessageMapping("/chat.leave")
    public void leave(@Header("userId") Long userId,
                      @Header("isManager") Boolean isManager) {
        chatPresenceService.setOffline(isManager, userId);
        // messagingTemplate.convertAndSend("/topic/presence." + userId, "offline");
    }
}
