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
        return chat(userQuestion, conversationHistory, null);
    }
    
    /**
     * RAG 기반 질의 응답 (사용자 ID 포함)
     */
    public RagResponse chat(String userQuestion, List<ChatMessageDto> conversationHistory, Long userId) {
        try {
            if (userQuestion == null || userQuestion.trim().isEmpty()) {
                log.warn("빈 사용자 질문 수신: RAG 검색 생략");
                return fallbackToZeroShot("어떤 점이 궁금하신가요? 질문을 조금만 더 자세히 알려주세요.", conversationHistory);
            }
            
            boolean isPersonalQuery = isPersonalInformationQuery(userQuestion);
            SearchResult searchResult;
            
            if (isPersonalQuery && userId == null) {
                log.info("비인증 개인정보 질문 감지: RAG 검색 생략");
                return loginRequiredResponse();
            }

            if (isPersonalQuery) {
                SearchResult userResult = vectorSearchService.searchUserData(userId, userQuestion);
                SearchResult publicResult = vectorSearchService.searchPublicOnly(userQuestion);
                searchResult = mergeSearchResults(userResult, publicResult);
                log.info("개인정보 질문 감지 - 사용자 {} 개인정보와 공개 정보 검색: 개인={}, 공개={}",
                    userId, userResult.getChunks().size(), publicResult.getChunks().size());
            } else {
                searchResult = vectorSearchService.searchPublicOnly(userQuestion);
                log.info("일반 질문 - 공개 정보만 검색: 결과={}", searchResult.getChunks().size());
            }
            
            boolean hasRelevantContext = !searchResult.getChunks().isEmpty() &&
                (isPersonalQuery || searchResult.getChunks().get(0).getSimilarity() > CONTEXT_REQUIRED_THRESHOLD);
            
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
     * 개인정보 관련 질문인지 확인
     */
    private boolean isPersonalInformationQuery(String question) {
        if (question == null) return false;
        
        String normalized = question.toLowerCase().replaceAll("\\s+", " ").trim();
        String compact = normalized.replace(" ", "");
        return normalized.contains("내 ") ||
               normalized.contains("나의 ") ||
               normalized.contains("내가 ") ||
               compact.contains("내예약") ||
               compact.contains("내예매") ||
               compact.contains("내티켓") ||
               compact.contains("예약내역") ||
               compact.contains("예매내역") ||
               compact.contains("결제내역") ||
               compact.contains("내결제") ||
               compact.contains("개인정보") ||
               compact.contains("프로필") ||
               compact.contains("마이페이지") ||
               compact.contains("내정보");
    }

    private SearchResult mergeSearchResults(SearchResult userResult, SearchResult publicResult) {
        List<SearchResult.ScoredChunk> chunks = new ArrayList<>();
        if (userResult != null && userResult.getChunks() != null) {
            chunks.addAll(userResult.getChunks());
        }
        if (publicResult != null && publicResult.getChunks() != null) {
            chunks.addAll(publicResult.getChunks());
        }

        String contextText = List.of(userResult, publicResult).stream()
            .filter(result -> result != null && result.getContextText() != null && !result.getContextText().isBlank())
            .map(SearchResult::getContextText)
            .collect(Collectors.joining("\n\n"));

        int totalChunks = 0;
        if (userResult != null) {
            totalChunks += userResult.getTotalChunks();
        }
        if (publicResult != null) {
            totalChunks += publicResult.getTotalChunks();
        }

        return SearchResult.builder()
            .chunks(chunks)
            .contextText(contextText)
            .totalChunks(totalChunks)
            .build();
    }

    private RagResponse loginRequiredResponse() {
        return RagResponse.builder()
            .answer("개인 예매내역이나 개인정보는 로그인한 본인에게만 안내할 수 있어요. 로그인 후 다시 물어봐 주세요.")
            .hasContext(false)
            .citedChunks(List.of())
            .totalSearched(0)
            .build();
    }
    
    /**
     * RAG 시스템 프롬프트 구성
     */
    private String buildRagSystemPrompt(String contextText) {
        return String.format("""
            안녕! 나는 '페어링'이야, FairPlay 플랫폼의 친근한 AI 도우미야! 
            사용자가 이벤트, 예약, 개인정보 등에 대해 궁금해하면 도움이 되는 정보를 제공해줄게!
            
            **참고할 정보:**
            %s
            
            **답변 가이드라인:**
            
            📋 **개인정보 관련 질문 (내 예약, 내 티켓, 내 정보 등)**:
            - "내 예약 내역", "내 티켓 정보", "내 개인정보" 등의 질문 시
            - 위 참고 정보에서 해당 사용자의 정보를 찾아 정확히 제공
            - 예약 상태, 이벤트명, 날짜, 가격 등 상세 정보 포함
            - 개인정보는 해당 사용자에게만 제공 (보안 유지)
            
            🎪 **이벤트 정보 관련 질문**:
            - 특정 이벤트, 축제, 부스에 대한 질문 시
            - 위 참고 정보에서 관련 내용을 찾아 제공
            - 이벤트명, 날짜, 장소, 설명, 티켓 정보 등 포함
            - 행사/부스 관리자 연락처와 이메일은 공개 문의 정보이므로 참고 정보에 있으면 제공
            - 이벤트 ID 같은 시스템 정보는 언급하지 말 것
            
            🎫 **부스 및 체험 정보**:
            - 특정 이벤트의 부스 정보나 체험 프로그램 질문 시
            - 부스명, 위치, 체험 내용, 소요시간, 참가자 수 등 제공
            
            ✅ **답변 원칙**:
            1. 참고 정보에 있는 내용만 정확히 제공
            2. 개인정보는 해당 사용자 것만 표시
            3. 정보가 없으면 솔직히 "찾을 수 없어요" 안내
            4. 친근하고 자연스러운 한국어로 답변
            5. 중요 정보는 구조화해서 보기 쉽게 정리
            6. 이벤트 ID, 문서 ID 등 시스템 정보는 절대 노출하지 말 것
            
            ❌ **금지사항**:
            - 참고 정보에 없는 내용 추측하지 말 것
            - 다른 사용자의 개인정보 노출하지 말 것
            - 부정확한 정보 제공하지 말 것
            - 이벤트 ID, 문서 ID, 청크 ID 등 시스템 정보 노출하지 말 것
            - 숫자로 된 식별자나 기술적 정보는 사용자에게 불필요
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
