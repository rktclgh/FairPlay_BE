// src/main/java/com/fairing/fairplay/ai/orchestrator/AiChatOrchestrator.java
package com.fairing.fairplay.ai.orchestrator;

import com.fairing.fairplay.ai.dto.AiChatMessageDto;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import com.fairing.fairplay.ai.rag.service.RagChatService;
import com.fairing.fairplay.ai.service.LlmRouter;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.entity.ChatMessage;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.event.ChatMessageCreatedEvent;
import com.fairing.fairplay.chat.repository.ChatMessageRepository;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import com.fairing.fairplay.chat.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class AiChatOrchestrator {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final LlmRouter llmRouter;
    private final RagChatService ragChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Executor aiResponseExecutor;

    // ✅ 봇 계정은 “로그인” 필요 없음. 서버가 DB에 저장할 때 senderId=botUserId로 넣어줌
    @Value("${llm.bot-user-id}")
    private Long botUserId;

    @Autowired
    public AiChatOrchestrator(
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            ChatMessageService chatMessageService,
            LlmRouter llmRouter,
            RagChatService ragChatService,
            SimpMessagingTemplate messagingTemplate,
            @Qualifier("aiChatTaskExecutor") Executor aiResponseExecutor
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageService = chatMessageService;
        this.llmRouter = llmRouter;
        this.ragChatService = ragChatService;
        this.messagingTemplate = messagingTemplate;
        this.aiResponseExecutor = aiResponseExecutor;
    }

    public AiChatOrchestrator(
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            ChatMessageService chatMessageService,
            LlmRouter llmRouter,
            RagChatService ragChatService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageService,
                llmRouter,
                ragChatService,
                messagingTemplate,
                createFallbackExecutor()
        );
    }

    private static Executor createFallbackExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadNumber = 1;

            @Override
            public synchronized Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-chat-response-test-" + threadNumber++);
                thread.setDaemon(true);
                return thread;
            }
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.prestartAllCoreThreads();
        return executor;
    }

    // 간단 루프 방지(봇이 쓴 메시지에 또 반응하지 않기)
    private boolean shouldRespond(Long senderId) {
        return !botUserId.equals(senderId);
    }

    @EventListener
    public void onUserMessage(ChatMessageCreatedEvent event) {
        if (!hasSavedMessagePayload(event)) {
            log.info("AI 응답 스킵: 저장된 사용자 메시지 payload가 없음. roomId={}, chatMessageId={}",
                    event.getChatRoomId(), event.getChatMessageId());
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runAfterTransaction(event);
                }
            });
            return;
        }

        handleUserMessage(event);
    }

    private void runAfterTransaction(ChatMessageCreatedEvent event) {
        try {
            if (aiResponseExecutor instanceof ThreadPoolExecutor fallbackExecutor) {
                Future<?> future = fallbackExecutor.submit(() -> handleUserMessageSafely(event));
                try {
                    future.get(20, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                    // 테스트 호환용 fallback 경로에서는 긴 AI 작업을 기다리지 않는다.
                }
                return;
            }
            aiResponseExecutor.execute(() -> handleUserMessageSafely(event));
        } catch (RuntimeException e) {
            log.error("AI 응답 작업 제출 실패. roomId={}", event.getChatRoomId(), e);
            sendErrorMessage(event.getChatRoomId(), "AI 응답 작업을 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            log.error("AI 응답 작업 확인 중 오류 발생. roomId={}", event.getChatRoomId(), e);
            sendErrorMessage(event.getChatRoomId(), "AI 응답 작업을 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private void handleUserMessageSafely(ChatMessageCreatedEvent event) {
        try {
            handleUserMessage(event);
        } catch (Exception e) {
            log.error("AI 응답 처리 중 오류 발생. roomId={}", event.getChatRoomId(), e);
            sendErrorMessage(event.getChatRoomId(), "AI 응답 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private void handleUserMessage(ChatMessageCreatedEvent event) {
        Long roomId = event.getChatRoomId();
        Long senderId = event.getSenderId();

        log.info("AI 오케스트레이터 이벤트 수신. roomId={}, senderId={}, chatMessageId={}",
                roomId, senderId, event.getChatMessageId());

        if (!shouldRespond(senderId)) {
            log.info("AI 응답 스킵: 봇이 보낸 메시지. roomId={}", roomId);
            return;
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));

        log.info("AI 채팅방 확인. roomId={}, targetType={}, targetId={}",
                roomId, room.getTargetType(), room.getTargetId());

        // AI 어시스던트 방만 처리
        if (room.getTargetType() != TargetType.AI) {
            log.info("AI 응답 스킵: AI 채팅방이 아님. roomId={}", roomId);
            return;
        }

        // 최근 대화 기록 구성
        List<ChatMessage> history = chatMessageRepository.findByChatRoomOrderBySentAtAsc(room);
        history.sort(Comparator.comparing(ChatMessage::getSentAt));

        ResolvedUserMessage userMessage = new ResolvedUserMessage(event.getChatMessageId(), event.getContent());

        // 대화 기록을 ChatMessageDto로 변환
        List<ChatMessageDto> conversationHistory = new ArrayList<>();
        List<ChatMessage> scopedHistory = historyUpToEventMessage(history, userMessage.chatMessageId());
        for (ChatMessage m : scopedHistory.subList(Math.max(0, scopedHistory.size() - 10), scopedHistory.size())) {
            if (m.getSenderId().equals(botUserId)) {
                conversationHistory.add(ChatMessageDto.assistant(m.getContent()));
            } else {
                conversationHistory.add(ChatMessageDto.user(m.getContent()));
            }
        }
        if (conversationHistory.isEmpty()
                || !userMessage.content().equals(conversationHistory.get(conversationHistory.size() - 1).getContent())) {
            conversationHistory.add(ChatMessageDto.user(userMessage.content()));
        }

        // RAG 기반 응답 생성 (사용자 ID 포함)
        String reply;
        try {
            log.info("RAG 채팅 호출 시작. roomId={}, userId={}, chatMessageId={}",
                    roomId, room.getUserId(), userMessage.chatMessageId());
            RagChatService.RagResponse ragResponse = ragChatService.chat(userMessage.content(), conversationHistory, room.getUserId());
            
            reply = ragResponse.getAnswer();
            
            log.info("RAG 응답 생성 완료. roomId={}, hasContext={}, citedChunks={}",
                    roomId, ragResponse.isHasContext(), ragResponse.getCitedChunks().size());
            
        } catch (Exception e) {
            log.warn("RAG 채팅 실패, 기본 LLM으로 fallback. roomId={}", roomId, e);
            
            // Fallback to original 0-shot approach
            List<ChatMessageDto> prompt = new ArrayList<>();
            prompt.add(ChatMessageDto.system("""
                안녕! 나는 '페어링'이야, FairPlay 플랫폼의 AI 도우미야. 
                사용자의 모든 질문에 친절하고 도움이 되는 답변을 해줄게!
                - 답변은 한국어로 자연스럽고 친근하게.
                - 일반적인 질문도 답변할 수 있어.
                - FairPlay 관련 질문이면 더 자세히 도와줄게.
                - 모르는 것은 솔직히 말하고 다른 방법을 제안할게.
                - 내 이름은 페어링이야! 기억해줘.
                """));
            
            prompt.addAll(conversationHistory.subList(Math.max(0, conversationHistory.size() - 6), conversationHistory.size()));
            
            try {
                reply = llmRouter.pick(null).chat(prompt, 0.6, 1024);
                if (reply == null || reply.isBlank()) {
                    reply = "죄송해요, 방금은 답변을 생성하지 못했어요. 한 번만 더 질문해줄래요?";
                }
            } catch (Exception e2) {
                log.error("기본 LLM fallback 실패. roomId={}", roomId, e2);
                reply = "지금은 답변 생성에 문제가 있어요. 잠시 후 다시 시도해주세요.";
            }
        }

        // ✅ 트랜잭션 분리해서 저장 및 방송
        saveAndBroadcast(roomId, reply);
    }

    private boolean hasSavedMessagePayload(ChatMessageCreatedEvent event) {
        return event.getChatMessageId() != null
                && event.getContent() != null
                && !event.getContent().isBlank();
    }

    private List<ChatMessage> historyUpToEventMessage(List<ChatMessage> history, Long chatMessageId) {
        if (chatMessageId == null) {
            return history;
        }

        for (int i = 0; i < history.size(); i++) {
            if (chatMessageId.equals(history.get(i).getChatMessageId())) {
                return history.subList(0, i + 1);
            }
        }
        return history;
    }

    @Transactional
    protected void saveAndBroadcast(Long roomId, String reply) {
        log.info("AI 메시지 저장 및 브로드캐스트. roomId={}, botUserId={}", roomId, botUserId);
        ChatMessageResponseDto saved = chatMessageService.sendMessage(roomId, botUserId, reply);
        messagingTemplate.convertAndSend("/topic/ai-chat." + roomId, saved);
        // 방 목록 업데이트 브로드캐스트(기존 패턴 맞춤)
        messagingTemplate.convertAndSend("/topic/chat-room-list", "UPDATE");
        log.info("AI 응답 처리 완료. roomId={}, chatMessageId={}", roomId, saved.getChatMessageId());
    }

    private void sendErrorMessage(Long roomId, String errorMessage) {
        AiChatMessageDto errorResponse = AiChatMessageDto.builder()
                .type("system_error")
                .content(errorMessage)
                .chatRoomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/ai-chat." + roomId, errorResponse);
    }

    private record ResolvedUserMessage(Long chatMessageId, String content) {
    }
}
