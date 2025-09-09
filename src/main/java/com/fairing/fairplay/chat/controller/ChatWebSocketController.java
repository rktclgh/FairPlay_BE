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
        // 1. 세션 속성에서 직접 추출 (가장 확실한 방법)
        if (headerAccessor.getSessionAttributes() != null) {
            Object userId = headerAccessor.getSessionAttributes().get("userId");
            if (userId instanceof Long) {
                log.debug("세션 속성에서 userId 추출 성공: {}", userId);
                return (Long) userId;
            }
        }
        
        // 2. Principal에서 추출 (fallback)
        if (principal != null && principal.getName() != null) {
            // StompPrincipal인 경우 (SessionHandshakeInterceptor에서 설정한 userId 문자열)
            if ("StompPrincipal".equals(principal.getClass().getSimpleName())) {
                try {
                    Long userId = Long.parseLong(principal.getName());
                    log.debug("StompPrincipal에서 userId 추출 성공: {}", userId);
                    return userId;
                } catch (NumberFormatException e) {
                    log.warn("StompPrincipal에서 userId 파싱 실패: {}", principal.getName());
                }
            } else {
                // 일반 Principal인 경우, userId 문자열 시도
                try {
                    Long userId = Long.parseLong(principal.getName());
                    log.debug("Principal.getName()에서 userId 추출 성공: {}", userId);
                    return userId;
                } catch (NumberFormatException e) {
                    log.debug("Principal.getName()을 userId로 파싱할 수 없음 (이메일 형식): {}", principal.getName());
                }
            }
        }
        
        // 3. 메시지 페이로드에서 추출 (보안상 권장하지 않음)
        if (message.getSenderId() != null) {
            log.debug("메시지에서 사용자 ID 사용: {}", message.getSenderId());
            return message.getSenderId();
        }
        
        log.warn("모든 방법으로 사용자 ID를 찾을 수 없음");
        return null;
    }

}
