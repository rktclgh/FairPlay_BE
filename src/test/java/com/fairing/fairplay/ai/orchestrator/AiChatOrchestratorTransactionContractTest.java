package com.fairing.fairplay.ai.orchestrator;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AiChatOrchestratorTransactionContractTest.TestConfig.class)
@TestPropertySource(properties = "llm.bot-user-id=999")
class AiChatOrchestratorTransactionContractTest {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    ChatMessageService chatMessageService;

    @Autowired
    RagChatService ragChatService;

    @Autowired
    LlmRouter llmRouter;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void resetMocks() {
        reset(chatRoomRepository, chatMessageRepository, chatMessageService, ragChatService, messagingTemplate);
    }

    @Test
    void invokesRagChatAfterCommitWithoutAnActiveTransaction() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom aiRoom = ChatRoom.builder()
                .chatRoomId(roomId)
                .userId(senderId)
                .targetType(TargetType.AI)
                .targetId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage userMessage = ChatMessage.builder()
                .chatMessageId(1L)
                .chatRoom(aiRoom)
                .senderId(senderId)
                .content("예매 방법 알려줘")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(aiRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(aiRoom)).thenReturn(new ArrayList<>(List.of(userMessage)));
        when(ragChatService.chat(eq("예매 방법 알려줘"), any(List.class), eq(senderId)))
                .thenAnswer(invocation -> {
                    assertFalse(
                            TransactionSynchronizationManager.isActualTransactionActive(),
                            "RAG/LLM 호출은 사용자 메시지 저장 트랜잭션 커밋 이후, 트랜잭션 밖에서 실행되어야 한다."
                    );
                    return RagChatService.RagResponse.builder()
                            .answer("예매는 행사 상세 페이지에서 할 수 있어요.")
                            .hasContext(false)
                            .citedChunks(List.of())
                            .totalSearched(0)
                            .build();
                });
        when(chatMessageService.sendMessage(roomId, 999L, "예매는 행사 상세 페이지에서 할 수 있어요."))
                .thenReturn(ChatMessageResponseDto.builder()
                        .chatMessageId(2L)
                        .chatRoomId(roomId)
                        .senderId(999L)
                        .content("예매는 행사 상세 페이지에서 할 수 있어요.")
                        .sentAt(LocalDateTime.now())
                        .isRead(false)
                        .build());

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId, 1L, "예매 방법 알려줘"))
        );

        verify(ragChatService).chat(eq("예매 방법 알려줘"), any(List.class), eq(senderId));
        verify(chatMessageService).sendMessage(roomId, 999L, "예매는 행사 상세 페이지에서 할 수 있어요.");
    }

    @Test
    void broadcastsAiResponseToAiChatRoomTopicSubscribedByFrontend() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom aiRoom = aiRoom(roomId, senderId);
        ChatMessage userMessage = userMessage(1L, aiRoom, senderId, "예매 방법 알려줘");
        ChatMessageResponseDto savedReply = savedBotMessage(2L, roomId, "예매는 행사 상세 페이지에서 할 수 있어요.");

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(aiRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(aiRoom)).thenReturn(new ArrayList<>(List.of(userMessage)));
        when(ragChatService.chat(eq("예매 방법 알려줘"), any(List.class), eq(senderId)))
                .thenReturn(ragAnswer("예매는 행사 상세 페이지에서 할 수 있어요."));
        when(chatMessageService.sendMessage(roomId, 999L, "예매는 행사 상세 페이지에서 할 수 있어요."))
                .thenReturn(savedReply);

        eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId, 1L, "예매 방법 알려줘"));

        verify(chatMessageService).sendMessage(roomId, 999L, "예매는 행사 상세 페이지에서 할 수 있어요.");
        verify(messagingTemplate).convertAndSend("/topic/ai-chat." + roomId, savedReply);
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat." + roomId), any(Object.class));
    }

    @Test
    void consecutiveEventsAnswerTheQuestionThatBelongsToEachEvent() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom aiRoom = aiRoom(roomId, senderId);
        ChatMessage firstQuestion = userMessage(1L, aiRoom, senderId, "첫 번째 질문");
        ChatMessage secondQuestion = userMessage(2L, aiRoom, senderId, "두 번째 질문");

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(aiRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(aiRoom))
                .thenReturn(new ArrayList<>(List.of(firstQuestion, secondQuestion)));
        when(ragChatService.chat(eq("첫 번째 질문"), any(List.class), eq(senderId)))
                .thenReturn(ragAnswer("첫 번째 답변"));
        when(ragChatService.chat(eq("두 번째 질문"), any(List.class), eq(senderId)))
                .thenReturn(ragAnswer("두 번째 답변"));
        when(chatMessageService.sendMessage(roomId, 999L, "첫 번째 답변"))
                .thenReturn(savedBotMessage(3L, roomId, "첫 번째 답변"));
        when(chatMessageService.sendMessage(roomId, 999L, "두 번째 답변"))
                .thenReturn(savedBotMessage(4L, roomId, "두 번째 답변"));

        eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId, 1L, "첫 번째 질문"));
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId, 2L, "두 번째 질문"));

        verify(ragChatService).chat(eq("첫 번째 질문"), any(List.class), eq(senderId));
        verify(ragChatService).chat(eq("두 번째 질문"), any(List.class), eq(senderId));
        verify(chatMessageService).sendMessage(roomId, 999L, "첫 번째 답변");
        verify(chatMessageService).sendMessage(roomId, 999L, "두 번째 답변");
    }

    @Test
    void afterCommitSchedulesAiResponseWithoutWaitingForCompletion() throws Exception {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom aiRoom = aiRoom(roomId, senderId);
        ChatMessage userMessage = userMessage(1L, aiRoom, senderId, "예매 방법 알려줘");
        CountDownLatch ragStarted = new CountDownLatch(1);
        CountDownLatch unblockRag = new CountDownLatch(1);
        CountDownLatch transactionReturned = new CountDownLatch(1);
        ExecutorService transactionThread = Executors.newSingleThreadExecutor();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(aiRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(aiRoom)).thenReturn(new ArrayList<>(List.of(userMessage)));
        when(ragChatService.chat(eq("예매 방법 알려줘"), any(List.class), eq(senderId)))
                .thenAnswer(invocation -> {
                    ragStarted.countDown();
                    assertTrue(unblockRag.await(2, TimeUnit.SECONDS));
                    return ragAnswer("예매는 행사 상세 페이지에서 할 수 있어요.");
                });
        when(chatMessageService.sendMessage(roomId, 999L, "예매는 행사 상세 페이지에서 할 수 있어요."))
                .thenReturn(savedBotMessage(2L, roomId, "예매는 행사 상세 페이지에서 할 수 있어요."));

        Future<?> transactionFuture = transactionThread.submit(() -> {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId, 1L, "예매 방법 알려줘"))
            );
            transactionReturned.countDown();
        });

        assertTrue(ragStarted.await(1, TimeUnit.SECONDS));
        try {
            assertTrue(
                    transactionReturned.await(100, TimeUnit.MILLISECONDS),
                    "afterCommit은 AI 응답 완료를 .join()으로 기다리지 않고 커밋 후 작업만 예약해야 한다."
            );
        } finally {
            unblockRag.countDown();
            transactionFuture.get(2, TimeUnit.SECONDS);
            transactionThread.shutdownNow();
        }
    }

    @Test
    void aiResponseExecutorIsInjectableInsteadOfStaticLifecycleOwnedThreadPool() {
        boolean hasStaticExecutorService = Arrays.stream(AiChatOrchestrator.class.getDeclaredFields())
                .anyMatch(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && ExecutorService.class.isAssignableFrom(field.getType()));

        assertFalse(
                hasStaticExecutorService,
                "AI 응답 executor는 Spring bean으로 주입되어 bounded/lifecycle 설정을 검증할 수 있어야 하며 static ExecutorService이면 안 된다."
        );
    }

    @Test
    void doesNotGenerateAiResponseWhenLegacyEventHasNoSavedMessagePayload() {
        Long roomId = 101L;
        Long senderId = 42L;
        ChatRoom aiRoom = aiRoom(roomId, senderId);
        ChatMessage userMessage = userMessage(1L, aiRoom, senderId, "history에 남은 질문");

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(aiRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(aiRoom)).thenReturn(new ArrayList<>(List.of(userMessage)));
        when(ragChatService.chat(eq("history에 남은 질문"), any(List.class), eq(senderId)))
                .thenReturn(ragAnswer("fallback으로 생성된 답변"));
        when(chatMessageService.sendMessage(roomId, 999L, "fallback으로 생성된 답변"))
                .thenReturn(savedBotMessage(2L, roomId, "fallback으로 생성된 답변"));

        eventPublisher.publishEvent(new ChatMessageCreatedEvent(roomId, senderId));

        verify(ragChatService, never()).chat(any(String.class), any(List.class), any(Long.class));
        verify(llmRouter, never()).pick(any());
        verify(chatMessageService, never()).sendMessage(any(Long.class), eq(999L), any(String.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/ai-chat." + roomId), any(Object.class));
    }

    private ChatRoom aiRoom(Long roomId, Long senderId) {
        return ChatRoom.builder()
                .chatRoomId(roomId)
                .userId(senderId)
                .targetType(TargetType.AI)
                .targetId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ChatMessage userMessage(Long messageId, ChatRoom room, Long senderId, String content) {
        return ChatMessage.builder()
                .chatMessageId(messageId)
                .chatRoom(room)
                .senderId(senderId)
                .content(content)
                .sentAt(LocalDateTime.now().plusNanos(messageId))
                .isRead(false)
                .build();
    }

    private RagChatService.RagResponse ragAnswer(String answer) {
        return RagChatService.RagResponse.builder()
                .answer(answer)
                .hasContext(false)
                .citedChunks(List.of())
                .totalSearched(0)
                .build();
    }

    private ChatMessageResponseDto savedBotMessage(Long messageId, Long roomId, String content) {
        return ChatMessageResponseDto.builder()
                .chatMessageId(messageId)
                .chatRoomId(roomId)
                .senderId(999L)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        AiChatOrchestrator aiChatOrchestrator(
                ChatRoomRepository chatRoomRepository,
                ChatMessageRepository chatMessageRepository,
                ChatMessageService chatMessageService,
                LlmRouter llmRouter,
                RagChatService ragChatService,
                SimpMessagingTemplate messagingTemplate
        ) {
            return new AiChatOrchestrator(
                    chatRoomRepository,
                    chatMessageRepository,
                    chatMessageService,
                    llmRouter,
                    ragChatService,
                    messagingTemplate
            );
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() throws TransactionException {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                }
            };
        }

        @Bean
        ChatRoomRepository chatRoomRepository() {
            return mock(ChatRoomRepository.class);
        }

        @Bean
        ChatMessageRepository chatMessageRepository() {
            return mock(ChatMessageRepository.class);
        }

        @Bean
        ChatMessageService chatMessageService() {
            return mock(ChatMessageService.class);
        }

        @Bean
        LlmRouter llmRouter() {
            return mock(LlmRouter.class);
        }

        @Bean
        RagChatService ragChatService() {
            return mock(RagChatService.class);
        }

        @Bean
        SimpMessagingTemplate messagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }
    }
}
