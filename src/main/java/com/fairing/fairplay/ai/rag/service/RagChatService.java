package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.dto.ChatMessageDto;
import com.fairing.fairplay.ai.rag.domain.SearchResult;
import com.fairing.fairplay.ai.service.LlmRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 기반 채팅 서비스
 * 기존 0-shot 모델과 연결하여 컨텍스트 기반 답변 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    private final VectorSearchService vectorSearchService;
    private final LlmRouter llmRouter;
    
    private static final double CONTEXT_REQUIRED_THRESHOLD = 0.4; // 0.2 → 0.4 (더 높은 품질의 컨텍스트만 사용)
    
    /**
     * RAG 기반 질의 응답
     */
    public RagResponse chat(String userQuestion, List<ChatMessageDto> conversationHistory) {
        try {
            if (userQuestion == null || userQuestion.trim().isEmpty()) {
                log.warn("빈 사용자 질문 수신: RAG 검색 생략");
                return fallbackToZeroShot("어떤 점이 궁금하신가요? 질문을 조금만 더 자세히 알려주세요.", conversationHistory);
            }
            
            // 벡터 검색으로 관련 컨텍스트 찾기
            SearchResult searchResult = vectorSearchService.search(userQuestion);
            
            boolean hasRelevantContext = !searchResult.getChunks().isEmpty() &&
                searchResult.getChunks().get(0).getSimilarity() > CONTEXT_REQUIRED_THRESHOLD;
            
            // 프롬프트 구성
            List<ChatMessageDto> prompt = new ArrayList<>();
            
            if (hasRelevantContext) {
                // RAG 시스템 프롬프트 (컨텍스트 포함)
                prompt.add(ChatMessageDto.system(buildRagSystemPrompt(searchResult.getContextText())));
            } else {
                // 일반 시스템 프롬프트
                prompt.add(ChatMessageDto.system("""
                    안녕! 나는 '페어링'이야, FairPlay 플랫폼의 AI 도우미야. 
                    사용자의 질문에 친절하고 도움이 되는 답변을 해줄게!
                    - 답변은 한국어로 자연스럽고 친근하게.
                    - 일반적인 질문도 답변할 수 있어.
                    - FairPlay 관련 질문이면 더 자세히 도와줄게.
                    - 모르는 것은 솔직히 말하고 다른 방법을 제안할게.
                    - 내 이름은 페어링이야! 기억해줘.
                    """));
            }
            
            // 대화 기록 추가 (최근 5개만)
            // 현재 질문은 conversationHistory의 마지막에 이미 포함되어 있으므로 중복 추가하지 않음
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int startIndex = Math.max(0, conversationHistory.size() - 5);
                prompt.addAll(conversationHistory.subList(startIndex, conversationHistory.size()));
            }
            
            // LLM 호출
            String response = llmRouter.pick(null).chat(prompt, 0.7, 1024);
            
            log.info("LLM 응답 길이: {} (null: {})", response != null ? response.length() : 0, response == null);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM에서 빈 응답 수신. fallback으로 전환");
                return fallbackToZeroShot(userQuestion, conversationHistory);
            }
            
            // 마크다운 형식 제거 (**text** -> text)
            response = response.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
            
            // 인용 정보 구성
            List<CitedChunk> citedChunks = searchResult.getChunks().stream()
                .map(chunk -> new CitedChunk(
                    chunk.getChunk().getChunkId(),
                    chunk.getChunk().getDocId(),
                    Math.round(chunk.getSimilarity() * 1000.0) / 1000.0,
                    chunk.getChunk().getText().length() > 100 ? 
                        chunk.getChunk().getText().substring(0, 100) + "..." : 
                        chunk.getChunk().getText()
                ))
                .collect(Collectors.toList());
            
            log.info("RAG 채팅 완료: 질문={}, 컨텍스트 사용={}, 인용={}", 
                userQuestion.length() > 50 ? userQuestion.substring(0, 50) + "..." : userQuestion,
                hasRelevantContext, citedChunks.size());
            
            return RagResponse.builder()
                .answer(response)
                .hasContext(hasRelevantContext)
                .citedChunks(citedChunks)
                .totalSearched(searchResult.getTotalChunks())
                .build();
                
        } catch (Exception e) {
            log.error("RAG 채팅 오류: {}", e.getMessage(), e);
            
            // 오류 시 기본 0-shot 응답 시도
            return fallbackToZeroShot(userQuestion, conversationHistory);
        }
    }
    
    /**
     * RAG 시스템 프롬프트 구성
     */
    private String buildRagSystemPrompt(String contextText) {
        return String.format("""
            안녕! 나는 '페어링'이야, FairPlay 플랫폼의 친근한 AI 도우미야. 
            사용자가 이벤트나 공연에 대해 궁금해하면, 마치 현지 가이드처럼 자연스럽고 도움이 되는 정보를 제공해줄게!
            
            **참고할 이벤트 정보:**
            %s
            
            **중요한 답변 규칙:**
            ⚠️ **반드시 위의 참고 정보와 사용자 질문의 관련성을 먼저 확인해야 합니다!**
            
            1. **관련성 확인**: 위 참고 정보가 사용자가 묻는 이벤트/축제와 직접적으로 관련이 있나요?
               - 만약 사용자가 "송도 맥주축제"를 물어봤는데 참고 정보가 예매/취소/환불 정책만 있다면 → "관련 없음"
               - 만약 사용자가 물어본 이벤트명이 참고 정보에 명시적으로 포함되어 있다면 → "관련 있음"
            
            2. **관련 있을 때**: 위 정보를 바탕으로 자연스럽게 답변
               - 날짜, 장소, 주요 내용을 중심으로 간결하게 정리
               - 예약이나 참가 방법 같은 실용적인 팁도 함께 제공
            
            3. **관련 없을 때**: 다음과 같이 정직하게 답변
               "죄송해요! 현재 제가 가지고 있는 정보에서는 [사용자가 물어본 이벤트명]에 대한 구체적인 정보를 찾을 수 없어요. 
               정확한 정보는 해당 이벤트의 공식 홈페이지나 주최측에 직접 문의하시는 것을 추천드려요! 😊"
            
            **절대 하지 말 것:**
            - 참고 정보에 없는 내용을 추측해서 만들어내지 말 것
            - 일반적인 축제 정보로 대충 답변하지 말 것
            """, contextText);
    }
    
    /**
     * 오류 시 기본 0-shot 응답으로 fallback
     */
    private RagResponse fallbackToZeroShot(String userQuestion, List<ChatMessageDto> conversationHistory) {
        try {
            List<ChatMessageDto> fallbackPrompt = new ArrayList<>();
            
            fallbackPrompt.add(ChatMessageDto.system("""
                너는 FairPlay 플랫폼의 AI 도우미야. 
                사용자의 질문에 친절하고 도움이 되는 답변을 해줘.
                지금은 문서 검색 기능에 문제가 있지만, 최선을 다해 답변해줄게.
                """));
            
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int startIndex = Math.max(0, conversationHistory.size() - 3);
                fallbackPrompt.addAll(conversationHistory.subList(startIndex, conversationHistory.size()));
                
                // 마지막 메시지가 현재 userQuestion과 동일하면 중복 추가 방지
                ChatMessageDto lastMsg = conversationHistory.get(conversationHistory.size() - 1);
                if (lastMsg != null && userQuestion.equals(lastMsg.getContent())) {
                    // 이미 포함되어 있으므로 추가하지 않음
                } else {
                    // 새로운 질문이므로 추가
                    fallbackPrompt.add(ChatMessageDto.user(userQuestion));
                }
            } else {
                // 대화 기록이 없으면 현재 질문 추가
                fallbackPrompt.add(ChatMessageDto.user(userQuestion));
            }
            
            String response = llmRouter.pick(null).chat(fallbackPrompt, 0.7, 512);
            
            if (response == null || response.trim().isEmpty()) {
                response = "죄송해요, 지금은 시스템에 일시적인 문제가 있어요. 잠시 후 다시 시도해주세요.";
            } else {
                // 마크다운 형식 제거
                response = response.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
            }
            
            return RagResponse.builder()
                .answer(response)
                .hasContext(false)
                .citedChunks(List.of())
                .totalSearched(0)
                .build();
                
        } catch (Exception e) {
            log.error("Fallback 응답도 실패", e);
            
            return RagResponse.builder()
                .answer("죄송해요, 지금은 답변 생성에 문제가 있어요. 잠시 후 다시 시도해주세요.")
                .hasContext(false)
                .citedChunks(List.of())
                .totalSearched(0)
                .build();
        }
    }
    
    /**
     * RAG 응답 결과
     */
    public static class RagResponse {
        private final String answer;
        private final boolean hasContext;
        private final List<CitedChunk> citedChunks;
        private final int totalSearched;
        
        private RagResponse(String answer, boolean hasContext, List<CitedChunk> citedChunks, int totalSearched) {
            this.answer = answer;
            this.hasContext = hasContext;
            this.citedChunks = citedChunks;
            this.totalSearched = totalSearched;
        }
        
        public static RagResponseBuilder builder() {
            return new RagResponseBuilder();
        }
        
        public String getAnswer() { return answer; }
        public boolean isHasContext() { return hasContext; }
        public List<CitedChunk> getCitedChunks() { return citedChunks; }
        public int getTotalSearched() { return totalSearched; }
        
        public static class RagResponseBuilder {
            private String answer;
            private boolean hasContext;
            private List<CitedChunk> citedChunks;
            private int totalSearched;
            
            public RagResponseBuilder answer(String answer) { this.answer = answer; return this; }
            public RagResponseBuilder hasContext(boolean hasContext) { this.hasContext = hasContext; return this; }
            public RagResponseBuilder citedChunks(List<CitedChunk> citedChunks) { this.citedChunks = citedChunks; return this; }
            public RagResponseBuilder totalSearched(int totalSearched) { this.totalSearched = totalSearched; return this; }
            
            public RagResponse build() {
                return new RagResponse(answer, hasContext, citedChunks, totalSearched);
            }
        }
    }
    
    /**
     * 인용된 청크 정보
     */
    public static class CitedChunk {
        private final String chunkId;
        private final String docId;
        private final double similarity;
        private final String snippet;
        
        public CitedChunk(String chunkId, String docId, double similarity, String snippet) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.similarity = similarity;
            this.snippet = snippet;
        }
        
        public String getChunkId() { return chunkId; }
        public String getDocId() { return docId; }
        public double getSimilarity() { return similarity; }
        public String getSnippet() { return snippet; }
    }
}