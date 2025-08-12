package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
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
    public void sendMessage(@Payload ChatMessageRequestDto message, Principal principal, 
                           SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 사용자 ID 결정
            Long senderId = determineSenderId(principal, message, headerAccessor);
            if (senderId == null) {
                log.warn("메시지 전송 실패: 사용자 ID를 확인할 수 없음. Room: {}", message.getChatRoomId());
                return;
            }
            
            log.debug("WebSocket 메시지 수신: 사용자 {} → 채팅방 {}", senderId, message.getChatRoomId());
            
            // 메시지 저장 및 응답 생성
            ChatMessageResponseDto response = chatMessageService.sendMessage(
                message.getChatRoomId(), senderId, message.getContent());
            
            // 채팅방 내 메시지 브로드캐스팅
            String topic = "/topic/chat." + message.getChatRoomId();
            messagingTemplate.convertAndSend(topic, response);
            
            // 채팅방 목록 업데이트 알림 브로드캐스팅
            messagingTemplate.convertAndSend("/topic/chat-room-list", "UPDATE");
            
            log.debug("메시지 브로드캐스팅 완료: {} → {}", senderId, topic);
            
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 다양한 소스에서 사용자 ID를 결정
     */
    private Long determineSenderId(Principal principal, ChatMessageRequestDto message, 
                                  SimpMessageHeaderAccessor headerAccessor) {
        // 1. Principal에서 추출 (가장 신뢰할 수 있음)
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                log.warn("Principal에서 사용자 ID 파싱 실패: {}", principal.getName());
            }
        }
        
        // 2. 헤더 속성에서 추출
        if (headerAccessor.getSessionAttributes() != null) {
            Object userId = headerAccessor.getSessionAttributes().get("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        
        // 3. 메시지 페이로드에서 추출 (보안상 권장하지 않음)
        if (message.getSenderId() != null) {
            log.debug("메시지에서 사용자 ID 사용: {}", message.getSenderId());
            return message.getSenderId();
        }
        
        return null;
    }

}
