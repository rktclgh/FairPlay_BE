package com.fairing.fairplay.ai.controller;

import com.fairing.fairplay.ai.dto.AiChatMessageDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AiChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/ai-chat.sendMessage")
    public void sendAiMessage(@Payload AiChatMessageDto message, Principal principal) {
        log.info("=== AI WebSocket 메시지 수신 ===");
        log.info("Principal: {}", principal);
        log.info("Message: {}", message);
        
        try {
            // 사용자 ID 추출
            Long senderId = authenticatedSenderId(principal);
            if (senderId == null) {
                sendErrorMessage(message.getChatRoomId(), "인증이 필요합니다.");
                return;
            }

            // 사용자 메시지 저장 이벤트가 커밋된 뒤 AiChatOrchestrator가 AI 응답을 생성한다.
            ChatMessageResponseDto userMessage = chatMessageService.sendMessage(
                message.getChatRoomId(), 
                senderId, 
                message.getContent()
            );
            
            String topic = "/topic/ai-chat." + message.getChatRoomId();
            messagingTemplate.convertAndSend(topic, userMessage);

            // 채팅방 목록 업데이트 알림
            messagingTemplate.convertAndSend("/topic/chat-room-list", "UPDATE");
            
            log.info("AI 채팅 사용자 메시지 처리 완료");

        } catch (Exception e) {
            log.error("AI 채팅 처리 중 오류 발생", e);
            sendErrorMessage(message.getChatRoomId(), "AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void sendErrorMessage(Long chatRoomId, String errorMessage) {
        AiChatMessageDto errorResponse = AiChatMessageDto.builder()
            .type("system_error")
            .content(errorMessage)
            .chatRoomId(chatRoomId)
            .build();
        
        String topic = "/topic/ai-chat." + chatRoomId;
        messagingTemplate.convertAndSend(topic, errorResponse);
    }

    private Long authenticatedSenderId(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        if (principal.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("AI WebSocket principal did not expose a user id: {}", principal.getName());
            return null;
        }
    }
}
