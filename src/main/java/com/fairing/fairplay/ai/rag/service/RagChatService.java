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
    
    private static final double CONTEXT_REQUIRED_THRESHOLD = 0.3;
    
    /**
     * RAG 기반 질의 응답
     */
    public RagResponse chat(String userQuestion, List<ChatMessageDto> conversationHistory) {
        try {
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
                    너는 FairPlay 플랫폼의 AI 도우미야. 
                    사용자의 질문에 친절하고 도움이 되는 답변을 해줘.
                    - 답변은 한국어로 자연스럽고 친근하게.
                    - 일반적인 질문도 답변할 수 있어.
                    - FairPlay 관련 질문이면 더 자세히 도와줘.
                    - 모르는 것은 솔직히 말하고 다른 방법을 제안해.
                    """));
            }
            
            // 대화 기록 추가 (최근 5개만)
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int startIndex = Math.max(0, conversationHistory.size() - 5);
                prompt.addAll(conversationHistory.subList(startIndex, conversationHistory.size()));
            }
            
            // 현재 질문 추가
            prompt.add(ChatMessageDto.user(userQuestion));
            
            // LLM 호출
            String response = llmRouter.pick(null).chat(prompt, 0.7, 1024);
            
            if (response == null || response.trim().isEmpty()) {
                response = "죄송해요, 지금은 답변을 생성하지 못했어요. 한 번만 더 질문해주실래요?";
            }
            
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
            너는 FairPlay 플랫폼의 AI 도우미야.
            아래 제공된 컨텍스트 정보를 바탕으로 사용자의 질문에 정확하고 친절하게 답변해줘.
            
            **제공된 컨텍스트:**
            %s
            
            **답변 가이드라인:**
            - 컨텍스트에 있는 정보를 우선적으로 활용해서 답변해줘
            - 컨텍스트에 없는 내용은 추측하지 말고 "제공된 정보에서는 확인할 수 없어요"라고 해줘
            - 답변은 한국어로 자연스럽고 친근하게
            - 구체적이고 실용적인 정보를 제공해줘
            - 필요하면 단계별로 설명해줘
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
            }
            
            fallbackPrompt.add(ChatMessageDto.user(userQuestion));
            
            String response = llmRouter.pick(null).chat(fallbackPrompt, 0.7, 512);
            
            if (response == null || response.trim().isEmpty()) {
                response = "죄송해요, 지금은 시스템에 일시적인 문제가 있어요. 잠시 후 다시 시도해주세요.";
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