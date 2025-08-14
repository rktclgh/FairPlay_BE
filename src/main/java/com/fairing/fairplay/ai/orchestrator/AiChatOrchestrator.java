// src/main/java/com/fairing/fairplay/ai/orchestrator/AiChatOrchestrator.java
package com.fairing.fairplay.ai.orchestrator;

import com.fairing.fairplay.ai.service.LlmRouter;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import com.fairing.fairplay.ai.rag.service.RagChatService;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.event.ChatMessageCreatedEvent;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import com.fairing.fairplay.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiChatOrchestrator {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final LlmRouter llmRouter;
    private final RagChatService ragChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ✅ 봇 계정은 “로그인” 필요 없음. 서버가 DB에 저장할 때 senderId=botUserId로 넣어줌
    @Value("${llm.bot-user-id}")
    private Long botUserId;

    // 간단 루프 방지(봇이 쓴 메시지에 또 반응하지 않기)
    private boolean shouldRespond(Long senderId) {
        return !senderId.equals(botUserId);
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onUserMessage(ChatMessageCreatedEvent event) {
        Long roomId = event.getChatRoomId();
        Long senderId = event.getSenderId();
        
        System.out.println("=== AI 오케스트레이터 이벤트 수신 ===");
        System.out.println("Room ID: " + roomId);
        System.out.println("Sender ID: " + senderId);
        System.out.println("Bot User ID: " + botUserId);

        if (!shouldRespond(senderId)) {
            System.out.println("AI 응답 스킵: 봇이 보낸 메시지");
            return;
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));

        System.out.println("채팅방 타입: " + room.getTargetType());
        System.out.println("타겟 ID: " + room.getTargetId());

        // AI 어시스던트 방만 처리
        if (room.getTargetType() != TargetType.AI) {
            System.out.println("AI 응답 스킵: AI 채팅방이 아님");
            return;
        }
        
        System.out.println("AI 응답 처리 시작...");

        // 최근 대화 기록 구성
        List<ChatMessage> history = chatMessageRepository.findByChatRoomOrderBySentAtAsc(room);
        if (history.isEmpty()) return;
        history.sort(Comparator.comparing(ChatMessage::getSentAt));

        // 대화 기록을 ChatMessageDto로 변환
        List<ChatMessageDto> conversationHistory = new ArrayList<>();
        for (ChatMessage m : history.subList(Math.max(0, history.size() - 10), history.size())) {
            if (m.getSenderId().equals(botUserId)) {
                conversationHistory.add(ChatMessageDto.assistant(m.getContent()));
            } else {
                conversationHistory.add(ChatMessageDto.user(m.getContent()));
            }
        }

        // 마지막 사용자 메시지 추출
        String userQuestion = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            if (!m.getSenderId().equals(botUserId)) {
                userQuestion = m.getContent();
                break;
            }
        }
        if (userQuestion == null || userQuestion.isBlank()) {
            System.out.println("AI 응답 스킵: 사용자 발화를 찾지 못함");
            return;
        }

        // RAG 기반 응답 생성
        String reply;
        try {
            System.out.println("RAG 채팅 호출 시작...");
            RagChatService.RagResponse ragResponse = ragChatService.chat(userQuestion, conversationHistory);
            
            reply = ragResponse.getAnswer();
            
            System.out.println("RAG 응답: " + reply);
            System.out.println("컨텍스트 사용: " + ragResponse.isHasContext());
            System.out.println("인용 청크: " + ragResponse.getCitedChunks().size());
            
        } catch (Exception e) {
            System.out.println("RAG 채팅 실패, 기본 LLM으로 fallback: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to original 0-shot approach
            List<ChatMessageDto> prompt = new ArrayList<>();
            prompt.add(ChatMessageDto.system("""
                너는 FairPlay 플랫폼의 AI 도우미야. 
                사용자의 모든 질문에 친절하고 도움이 되는 답변을 해줘.
                - 답변은 한국어로 자연스럽고 친근하게.
                - 일반적인 질문도 답변할 수 있어.
                - FairPlay 관련 질문이면 더 자세히 도와줘.
                - 모르는 것은 솔직히 말하고 다른 방법을 제안해.
                """));
            
            prompt.addAll(conversationHistory.subList(Math.max(0, conversationHistory.size() - 6), conversationHistory.size()));
            
            try {
                reply = llmRouter.pick(null).chat(prompt, 0.6, 1024);
                if (reply == null || reply.isBlank()) {
                    reply = "죄송해요, 방금은 답변을 생성하지 못했어요. 한 번만 더 질문해줄래요?";
                }
            } catch (Exception e2) {
                reply = "지금은 답변 생성에 문제가 있어요. 잠시 후 다시 시도해주세요.";
            }
        }

        System.out.println("최종 AI 응답: " + reply);
        // ✅ 트랜잭션 분리해서 저장 및 방송
        saveAndBroadcast(roomId, reply);
    }

    @Transactional
    protected void saveAndBroadcast(Long roomId, String reply) {
        System.out.println("AI 메시지 저장 및 브로드캐스트...");
        System.out.println("Room ID: " + roomId + ", Bot User ID: " + botUserId);
        ChatMessageResponseDto saved = chatMessageService.sendMessage(roomId, botUserId, reply);
        System.out.println("저장된 메시지 ID: " + saved.getChatMessageId());
        messagingTemplate.convertAndSend("/topic/chat." + roomId, saved);
        System.out.println("WebSocket 브로드캐스트 완료");
        // 방 목록 업데이트 브로드캐스트(기존 패턴 맞춤)
        messagingTemplate.convertAndSend("/topic/chat-room-list", "UPDATE");
        System.out.println("=== AI 응답 처리 완료 ===");
    }
}
