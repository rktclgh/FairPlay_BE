package com.fairing.fairplay.ai.controller;

import com.fairing.fairplay.ai.dto.AiChatMessageDto;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import com.fairing.fairplay.ai.dto.ChatRequestDto;
import com.fairing.fairplay.ai.service.ChatAiService;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AiChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatAiService chatAiService;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/ai-chat.sendMessage")
    public void sendAiMessage(@Payload AiChatMessageDto message, Principal principal) {
        log.info("=== AI WebSocket 메시지 수신 ===");
        log.info("Principal: {}", principal);
        log.info("Message: {}", message);
        
        try {
            // 사용자 ID 추출
            Long senderId;
            if (principal != null) {
                senderId = Long.parseLong(principal.getName());
            } else if (message.getSenderId() != null) {
                senderId = message.getSenderId();
            } else {
                sendErrorMessage(message.getChatRoomId(), "인증이 필요합니다.");
                return;
            }

            // 1. 사용자 메시지를 DB에 저장하고 브로드캐스트
            ChatMessageResponseDto userMessage = chatMessageService.sendMessage(
                message.getChatRoomId(), 
                senderId, 
                message.getContent()
            );
            
            // 사용자 메시지 브로드캐스트
            AiChatMessageDto userResponse = AiChatMessageDto.builder()
                .type("user_message")
                .content(message.getContent())
                .chatRoomId(message.getChatRoomId())
                .senderId(senderId)
                .timestamp(LocalDateTime.now().toString())
                .build();
            
            String topic = "/topic/ai-chat." + message.getChatRoomId();
            messagingTemplate.convertAndSend(topic, userResponse);

            // 2. AI 응답 생성을 위한 메시지 구성
            List<ChatMessageDto> chatMessages = new ArrayList<>();
            chatMessages.add(ChatMessageDto.builder()
                .role(ChatMessageDto.Role.SYSTEM)
                .content("당신은 FairPlay 이벤트 플랫폼의 도움이 되는 AI 어시스턴트입니다. 한국어로 친근하고 도움이 되는 답변을 해주세요.")
                .build());
            
            chatMessages.add(ChatMessageDto.builder()
                .role(ChatMessageDto.Role.USER)
                .content(message.getContent())
                .build());

            // 3. AI 서비스 호출
            ChatRequestDto aiRequest = ChatRequestDto.builder()
                .messages(chatMessages)
                .temperature(message.getTemperature() != null ? message.getTemperature() : 0.7)
                .maxOutputTokens(message.getMaxOutputTokens() != null ? message.getMaxOutputTokens() : 2048)
                .providerOverride(message.getProvider())
                .build();

            String aiResponse = chatAiService.chat(aiRequest);

            // 4. AI 응답을 DB에 저장 (봇 계정으로)
            Long botUserId = 999L; // application.properties의 llm.bot-user-id 값
            
            ChatMessageResponseDto aiMessage = chatMessageService.sendMessage(
                message.getChatRoomId(),
                botUserId,
                aiResponse
            );

            // 5. AI 응답 브로드캐스트
            AiChatMessageDto aiResponseDto = AiChatMessageDto.builder()
                .type("ai_response")
                .content(aiResponse)
                .chatRoomId(message.getChatRoomId())
                .senderId(botUserId)
                .timestamp(LocalDateTime.now().toString())
                .provider(message.getProvider() != null ? message.getProvider() : "gemini")
                .build();

            messagingTemplate.convertAndSend(topic, aiResponseDto);

            // 6. 채팅방 목록 업데이트 알림
            messagingTemplate.convertAndSend("/topic/chat-room-list", "UPDATE");
            
            log.info("AI 채팅 처리 완료");

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
            .timestamp(LocalDateTime.now().toString())
            .build();
        
        String topic = "/topic/ai-chat." + chatRoomId;
        messagingTemplate.convertAndSend(topic, errorResponse);
    }
}