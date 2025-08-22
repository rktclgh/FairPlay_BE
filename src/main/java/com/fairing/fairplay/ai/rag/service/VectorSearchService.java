package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.SearchResult;
import com.fairing.fairplay.ai.rag.repository.RagRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 벡터 검색 서비스 (코사인 유사도 기반)
 * 메모리 캐시를 통한 빠른 검색 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final RagRedisRepository repository;
    private final EmbeddingService embeddingService;
    
    // 메모리 캐시 (첫 검색 시 로드)
    private final Map<String, Chunk> chunkCache = new ConcurrentHashMap<>();
    private volatile boolean cacheInitialized = false;
    
    // 검색 설정 (한글 검색 지원을 위해 임계값 완화)
    private static final int DEFAULT_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.1; // 한글 임베딩을 위해 더욱 낮춤: 0.1
    private static final int MAX_CONTEXT_LENGTH = 2000;
    
    /**
     * 질의 텍스트로 관련 청크 검색
     */
    public SearchResult search(String query, int topK) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("")
                .totalChunks(0)
                .build();
        }
        
        // 캐시 초기화
        initializeCacheIfNeeded();
        
        // 벡터 검색 결과
        SearchResult vectorResult = performVectorSearch(query, topK);
        
        // 벡터 검색 결과가 충분하지 않거나 유사도가 낮으면 키워드 검색으로 보완
        boolean needKeywordSearch = vectorResult.getChunks().size() < topK / 2;
        if (!needKeywordSearch && !vectorResult.getChunks().isEmpty()) {
            // 최고 유사도가 0.5 미만이면 키워드 검색도 시도 (한글 검색 지원을 위해 완화)
            double maxSimilarity = vectorResult.getChunks().get(0).getSimilarity();
            needKeywordSearch = maxSimilarity < 0.5;
        }
        
        // 한글이 포함된 쿼리는 항상 키워드 검색도 수행 (한글 임베딩 성능 보완)
        if (containsKorean(query)) {
            needKeywordSearch = true;
            log.debug("한글 쿼리 감지 - 키워드 검색 강제 활성화: {}", query);
        }
        
        if (needKeywordSearch) {
            SearchResult keywordResult = performKeywordSearch(query, topK);
            return combineSearchResults(vectorResult, keywordResult, topK, query);
        }
        
        return vectorResult;
    }
    
    /**
     * 벡터 기반 검색
     */
    private SearchResult performVectorSearch(String query, int topK) throws Exception {
        
        if (chunkCache.isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("검색할 수 있는 문서가 없습니다.")
                .totalChunks(0)
                .build();
        }
        
        // 질의 임베딩 (검색용 최적화)
        float[] queryEmbedding = embeddingService.embedQuery(query);
        log.debug("질의 임베딩 생성 완료: {} 차원", queryEmbedding.length);
        
        // 모든 청크와 유사도 계산
        List<SearchResult.ScoredChunk> scoredChunks = new ArrayList<>();
        
        for (Chunk chunk : chunkCache.values()) {
            if (chunk.getEmbedding() == null) {
                log.warn("청크 {}에 임베딩이 없습니다.", chunk.getChunkId());
                continue;
            }
            
            double similarity = embeddingService.calculateCosineSimilarity(
                queryEmbedding, chunk.getEmbedding()
            );
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                scoredChunks.add(SearchResult.ScoredChunk.builder()
                    .chunk(chunk)
                    .similarity(similarity)
                    .build());
            }
        }
        
        // 유사도 순 정렬 및 Top-K 선택
        List<SearchResult.ScoredChunk> topChunks = scoredChunks.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());
        
        // 컨텍스트 텍스트 구성
        String contextText = buildContextText(topChunks);
        
        log.info("검색 완료: 총 {} 청크 중 {} 개 반환 (임계치: {})", 
            chunkCache.size(), topChunks.size(), SIMILARITY_THRESHOLD);
            
        // 송도 맥주축제 디버깅: event_7이 결과에 있는지 확인
        boolean foundBeerFestival = topChunks.stream()
            .anyMatch(chunk -> chunk.getChunk().getDocId().equals("event_7"));
        if (!foundBeerFestival) {
            log.warn("송도 맥주축제(event_7)가 검색 결과에 없음 - 모든 청크와 유사도 분석 필요");
            // 모든 청크의 유사도 로깅
            for (SearchResult.ScoredChunk sc : scoredChunks) {
                if (sc.getChunk().getDocId().equals("event_7")) {
                    log.warn("송도 맥주축제 유사도: {} (임계치: {})", sc.getSimilarity(), SIMILARITY_THRESHOLD);
                }
            }
        }
        
        // 상위 결과들의 유사도와 제목 로깅
        for (int i = 0; i < Math.min(3, topChunks.size()); i++) {
            SearchResult.ScoredChunk sc = topChunks.get(i);
            String title = sc.getChunk().getText().length() > 50 ? 
                sc.getChunk().getText().substring(0, 50) + "..." : sc.getChunk().getText();
            log.info("검색 결과 {}: 유사도={}, 제목={}", i+1, String.format("%.3f", sc.getSimilarity()), title);
        }
        
        return SearchResult.builder()
            .chunks(topChunks)
            .contextText(contextText)
            .totalChunks(chunkCache.size())
            .build();
    }
    
    /**
     * 기본 Top-K로 검색
     */
    public SearchResult search(String query) throws Exception {
        return search(query, DEFAULT_TOP_K);
    }
    
    /**
     * 사용자별 개인정보 검색 (특정 사용자 문서만 대상)
     */
    public SearchResult searchUserData(Long userId, String query) throws Exception {
        if (userId == null || query == null || query.trim().isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("")
                .totalChunks(0)
                .build();
        }
        
        // 캐시 초기화
        initializeCacheIfNeeded();
        
        // 해당 사용자의 문서만 필터링
        String userDocPrefix = "user_" + userId;
        List<Chunk> userChunks = chunkCache.values().stream()
            .filter(chunk -> chunk.getDocId().equals(userDocPrefix))
            .collect(Collectors.toList());
        
        log.info("사용자 {} 전용 청크 개수: {}", userId, userChunks.size());
        
        if (userChunks.isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("해당 사용자의 정보를 찾을 수 없습니다.")
                .totalChunks(0)
                .build();
        }
        
        // 질의 임베딩 생성
        float[] queryEmbedding = embeddingService.embedQuery(query);
        
        // 사용자 청크와 유사도 계산
        List<SearchResult.ScoredChunk> scoredChunks = new ArrayList<>();
        
        for (Chunk chunk : userChunks) {
            if (chunk.getEmbedding() == null) {
                log.warn("사용자 청크 {}에 임베딩이 없습니다.", chunk.getChunkId());
                continue;
            }
            
            double similarity = embeddingService.calculateCosineSimilarity(
                queryEmbedding, chunk.getEmbedding()
            );
            
            // 개인정보 검색은 임계값을 낮춤 (더 많은 관련 정보 반환)
            if (similarity >= 0.05) {
                scoredChunks.add(SearchResult.ScoredChunk.builder()
                    .chunk(chunk)
                    .similarity(similarity)
                    .build());
            }
        }
        
        // 유사도 순 정렬 (개인정보는 모든 관련 정보 반환)
        List<SearchResult.ScoredChunk> topChunks = scoredChunks.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .collect(Collectors.toList());
        
        // 개인정보 컨텍스트 구성 (모든 사용자 정보 포함)
        StringBuilder contextBuilder = new StringBuilder();
        for (Chunk chunk : userChunks) {
            contextBuilder.append(chunk.getText()).append("\n\n");
        }
        
        String contextText = contextBuilder.toString();
        if (contextText.length() > MAX_CONTEXT_LENGTH) {
            contextText = contextText.substring(0, MAX_CONTEXT_LENGTH) + "\n... (내용이 더 있습니다)";
        }
        
        log.info("사용자 {} 개인정보 검색 완료: 매칭 청크 {}, 컨텍스트 길이 {}", 
            userId, topChunks.size(), contextText.length());
        
        return SearchResult.builder()
            .chunks(topChunks)
            .contextText(contextText)
            .totalChunks(userChunks.size())
            .build();
    }
    
    /**
     * 캐시 초기화 (첫 요청 시 또는 수동)
     */
    public void initializeCache() {
        log.info("청크 캐시 초기화 시작...");
        
        List<Chunk> chunks = repository.findAllChunks();
        chunkCache.clear();
        
        for (Chunk chunk : chunks) {
            chunkCache.put(chunk.getChunkId(), chunk);
        }
        
        cacheInitialized = true;
        log.info("청크 캐시 초기화 완료: {} 개 청크 로드", chunkCache.size());
        
        // 로드된 청크들의 상세 분석 로깅 (디버깅용)
        Map<String, Integer> docTypeCount = new HashMap<>();
        int eventCount = 0;
        int songdoCount = 0;
        
        for (Chunk chunk : chunkCache.values()) {
            String docId = chunk.getDocId();
            String docType = docId.split("_")[0]; // event_, category_, booth_ 등 추출
            docTypeCount.put(docType, docTypeCount.getOrDefault(docType, 0) + 1);
            
            // 송도 관련 청크 찾기
            if (chunk.getText().toLowerCase().contains("송도") || 
                chunk.getText().toLowerCase().contains("맥주") ||
                chunk.getText().toLowerCase().contains("beer")) {
                songdoCount++;
                String title = chunk.getText().length() > 150 ? 
                    chunk.getText().substring(0, 150) + "..." : chunk.getText();
                log.info("🍺 송도/맥주 관련 청크 발견: docId={}, 내용={}", chunk.getDocId(), title);
            }
            
            // 이벤트 관련 청크 (일반)
            if ((chunk.getText().contains("이벤트") || chunk.getText().contains("축제") || chunk.getText().contains("제목")) && eventCount < 5) {
                String title = chunk.getText().length() > 100 ? 
                    chunk.getText().substring(0, 100) + "..." : chunk.getText();
                log.info("📅 이벤트 청크: docId={}, 내용={}", chunk.getDocId(), title);
                eventCount++;
            }
        }
        
        log.info("🔍 RAG 로드 상태 분석:");
        log.info("  - 총 청크 수: {}", chunkCache.size());
        log.info("  - 송도/맥주 관련 청크: {}개", songdoCount);
        for (Map.Entry<String, Integer> entry : docTypeCount.entrySet()) {
            log.info("  - {} 타입: {}개", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 캐시 무효화
     */
    public void invalidateCache() {
        chunkCache.clear();
        cacheInitialized = false;
        log.info("청크 캐시 무효화 완료");
    }
    
    /**
     * 모든 임베딩 데이터 삭제 (차원 변경 시 사용)
     */
    public void clearAllEmbeddingData() {
        repository.clearAllData();
        invalidateCache();
        log.info("모든 임베딩 데이터 삭제 완료 - 차원 변경으로 인한 데이터 재생성 필요");
    }
    
    /**
     * 캐시가 초기화되지 않았다면 초기화
     */
    private void initializeCacheIfNeeded() {
        if (!cacheInitialized) {
            synchronized (this) {
                if (!cacheInitialized) {
                    initializeCache();
                }
            }
        }
    }
    
    /**
     * Top-K 청크들로부터 컨텍스트 텍스트 구성
     */
    private String buildContextText(List<SearchResult.ScoredChunk> scoredChunks) {
        if (scoredChunks.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        int currentLength = 0;
        
        for (int i = 0; i < scoredChunks.size(); i++) {
            SearchResult.ScoredChunk scoredChunk = scoredChunks.get(i);
            String chunkText = scoredChunk.getChunk().getText();
            
            // 컨텍스트 구분자 제거 (사용자에게 보이지 않도록)
            String chunkWithSeparator = chunkText + "\n\n";
            
            // 최대 길이 초과 확인
            if (currentLength + chunkWithSeparator.length() > MAX_CONTEXT_LENGTH) {
                if (i == 0) {
                    // 첫 번째 청크라도 너무 길면 잘라서 포함
                    int remainingLength = MAX_CONTEXT_LENGTH - 20;
                    if (remainingLength > 100) {
                        String truncated = chunkText.length() > remainingLength ? 
                            chunkText.substring(0, remainingLength) + "..." : chunkText;
                        context.append(truncated).append("\n\n");
                    }
                }
                break;
            }
            
            context.append(chunkWithSeparator);
            currentLength += chunkWithSeparator.length();
        }
        
        return context.toString().trim();
    }
    
    /**
     * 키워드 기반 검색 (벡터 검색 보완용) - 한글 최적화
     */
    private SearchResult performKeywordSearch(String query, int topK) {
        List<SearchResult.ScoredChunk> keywordMatches = new ArrayList<>();
        
        // 한글과 영어 처리를 분리하여 검색 정확도 향상
        String normalizedQuery = query.trim();
        String[] keywords = normalizedQuery.split("\\s+");
        
        log.debug("키워드 검색 시작: 질의='{}', 키워드={}", query, String.join(", ", keywords));
        
        for (Chunk chunk : chunkCache.values()) {
            String chunkText = chunk.getText();
            double score = 0.0;
            
            // 각 키워드별로 점수 계산
            for (String keyword : keywords) {
                if (keyword.length() < 1) continue; // 한글은 1글자도 의미있을 수 있음
                
                // 1. 정확한 매칭 (대소문자 구분 없음)
                if (containsIgnoreCase(chunkText, keyword)) {
                    score += 10.0; // 정확한 매칭 시 높은 점수
                    
                    // 제목, 이벤트명, 검색 키워드 영역에서 발견된 경우 추가 점수
                    if (isInImportantField(chunkText, keyword)) {
                        score += 20.0; // 중요 필드에서 발견 시 매우 높은 점수
                        log.debug("중요 필드에서 키워드 '{}' 발견: {}", keyword, chunk.getChunkId());
                    }
                }
                
                // 2. 한글 부분 매칭 (2글자 이상)
                if (keyword.length() >= 2 && isKorean(keyword)) {
                    // 한글 키워드의 부분 문자열 검색
                    for (int i = 0; i <= keyword.length() - 2; i++) {
                        String partial = keyword.substring(i, Math.min(i + 2, keyword.length()));
                        if (containsIgnoreCase(chunkText, partial)) {
                            score += 2.0; // 부분 매칭 점수
                        }
                    }
                }
                
                // 3. 영어 부분 매칭 (3글자 이상)
                if (keyword.length() >= 3 && isEnglish(keyword)) {
                    for (int i = 0; i <= keyword.length() - 3; i++) {
                        String partial = keyword.substring(i, Math.min(i + 3, keyword.length()));
                        if (containsIgnoreCase(chunkText, partial)) {
                            score += 1.0; // 영어 부분 매칭 점수
                        }
                    }
                }
            }
            
            // 4. 전체 쿼리 완전 매칭 보너스
            if (containsIgnoreCase(chunkText, normalizedQuery)) {
                score += 50.0; // 전체 쿼리 매칭 시 최고 점수
                log.debug("전체 쿼리 '{}' 매칭: {}", normalizedQuery, chunk.getChunkId());
            }
            
            // 점수가 있는 경우만 결과에 포함
            if (score > 0) {
                double normalizedScore = Math.min(score / keywords.length, 100.0); // 최대 100으로 제한
                keywordMatches.add(SearchResult.ScoredChunk.builder()
                    .chunk(chunk)
                    .similarity(normalizedScore)
                    .build());
                
                if (score > 10) { // 유의미한 점수인 경우 로깅
                    log.debug("키워드 매칭: docId={}, 점수={}, 키워드='{}'", 
                        chunk.getDocId(), String.format("%.1f", normalizedScore), normalizedQuery);
                }
            }
        }
        
        // 점수순 정렬
        List<SearchResult.ScoredChunk> topKeywordChunks = keywordMatches.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());
        
        log.info("키워드 검색 완료: 질의='{}', 총 매칭={}개, 반환={}개", 
            query, keywordMatches.size(), topKeywordChunks.size());
        
        return SearchResult.builder()
            .chunks(topKeywordChunks)
            .contextText(buildContextText(topKeywordChunks))
            .totalChunks(chunkCache.size())
            .build();
    }
    
    /**
     * 대소문자 구분 없는 포함 검사 (한글/영어 모두 지원)
     */
    private boolean containsIgnoreCase(String text, String keyword) {
        return text.toLowerCase().contains(keyword.toLowerCase());
    }
    
    /**
     * 중요 필드에서의 키워드 발견 여부 확인
     */
    private boolean isInImportantField(String chunkText, String keyword) {
        String lowerText = chunkText.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        // 제목, 이벤트명, 검색 키워드 라인에서 키워드 검색
        String[] importantLines = lowerText.split("\n");
        for (String line : importantLines) {
            if ((line.contains("제목") || line.contains("이벤트명") || line.contains("검색 키워드")) 
                && line.contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 한글 텍스트 여부 확인
     */
    private boolean isKorean(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 텍스트에 한글이 포함되어 있는지 확인
     */
    private boolean containsKorean(String text) {
        return isKorean(text);
    }
    
    /**
     * 영어 텍스트 여부 확인
     */
    private boolean isEnglish(String text) {
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) && c <= 127) { // ASCII 영어 문자
                return true;
            }
        }
        return false;
    }
    
    /**
     * 벡터 검색과 키워드 검색 결과 결합 (한글 최적화)
     */
    private SearchResult combineSearchResults(SearchResult vectorResult, SearchResult keywordResult, int topK, String query) {
        Set<String> seenChunkIds = new HashSet<>();
        List<SearchResult.ScoredChunk> combinedChunks = new ArrayList<>();
        
        // 한글 쿼리인지 확인
        boolean isKoreanQuery = containsKorean(query);
        
        if (isKoreanQuery && !keywordResult.getChunks().isEmpty()) {
            log.debug("한글 쿼리 - 키워드 검색 결과 우선 처리");
            
            // 한글 쿼리의 경우 키워드 검색 결과를 우선하고 가중치 부여
            for (SearchResult.ScoredChunk chunk : keywordResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId())) {
                    // 키워드 검색 점수에 가중치 적용 (한글 검색 최적화)
                    SearchResult.ScoredChunk boostedChunk = SearchResult.ScoredChunk.builder()
                        .chunk(chunk.getChunk())
                        .similarity(Math.min(chunk.getSimilarity() * 1.5, 1.0)) // 1.5배 가중치
                        .build();
                    combinedChunks.add(boostedChunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
            
            // 그 다음 벡터 검색 결과 추가
            for (SearchResult.ScoredChunk chunk : vectorResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId()) && combinedChunks.size() < topK) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
        } else {
            log.debug("영어 쿼리 - 벡터 검색 결과 우선 처리");
            
            // 영어 쿼리의 경우 기존 방식: 벡터 검색 우선
            for (SearchResult.ScoredChunk chunk : vectorResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId())) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
            
            // 키워드 검색 결과 보완 추가
            for (SearchResult.ScoredChunk chunk : keywordResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId()) && combinedChunks.size() < topK) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
        }
        
        // 최종 점수순 정렬
        combinedChunks = combinedChunks.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());
        
        log.info("검색 결과 결합 완료: 벡터={}개, 키워드={}개, 최종={}개, 한글쿼리={}",
            vectorResult.getChunks().size(), keywordResult.getChunks().size(), 
            combinedChunks.size(), isKoreanQuery);
        
        return SearchResult.builder()
            .chunks(combinedChunks)
            .contextText(buildContextText(combinedChunks))
            .totalChunks(chunkCache.size())
            .build();
    }
    
    /**
     * 공개 정보만 검색 (사용자 개인정보 제외)
     * 일반 질문에 대해 공개된 이벤트, 부스 정보만 검색
     */
    public SearchResult searchPublicOnly(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("")
                .totalChunks(0)
                .build();
        }
        
        // 캐시 초기화
        initializeCacheIfNeeded();
        
        // 사용자 개인정보(user_xxx) 문서를 제외한 공개 정보만 필터링
        List<Chunk> publicChunks = chunkCache.values().stream()
            .filter(chunk -> !chunk.getDocId().startsWith("user_"))
            .collect(Collectors.toList());
        
        log.info("공개 정보 전용 청크 개수: {} (전체: {})", publicChunks.size(), chunkCache.size());
        
        if (publicChunks.isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("검색할 수 있는 공개 정보가 없습니다.")
                .totalChunks(0)
                .build();
        }
        
        // 질의 임베딩 생성
        float[] queryEmbedding = embeddingService.embedQuery(query);
        
        // 공개 청크와 유사도 계산
        List<SearchResult.ScoredChunk> scoredChunks = new ArrayList<>();
        
        for (Chunk chunk : publicChunks) {
            if (chunk.getEmbedding() == null) {
                log.warn("공개 청크 {}에 임베딩이 없습니다.", chunk.getChunkId());
                continue;
            }
            
            double similarity = embeddingService.calculateCosineSimilarity(
                queryEmbedding, chunk.getEmbedding()
            );
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                scoredChunks.add(SearchResult.ScoredChunk.builder()
                    .chunk(chunk)
                    .similarity(similarity)
                    .build());
            }
        }
        
        // 공개 정보도 키워드 검색으로 보완 (한글 지원)
        boolean needKeywordSearch = scoredChunks.size() < DEFAULT_TOP_K / 2;
        if (!needKeywordSearch && !scoredChunks.isEmpty()) {
            double maxSimilarity = scoredChunks.get(0).getSimilarity();
            needKeywordSearch = maxSimilarity < 0.5;
        }
        
        // 한글 쿼리는 항상 키워드 검색 추가
        if (containsKorean(query)) {
            needKeywordSearch = true;
        }
        
        if (needKeywordSearch) {
            // 공개 정보만 대상으로 키워드 검색
            SearchResult keywordResult = performPublicKeywordSearch(query, publicChunks, DEFAULT_TOP_K);
            return combinePublicSearchResults(scoredChunks, keywordResult, DEFAULT_TOP_K, query);
        }
        
        // 유사도 순 정렬 및 Top-K 선택
        List<SearchResult.ScoredChunk> topChunks = scoredChunks.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(DEFAULT_TOP_K)
            .collect(Collectors.toList());
        
        String contextText = buildContextText(topChunks);
        
        log.info("공개 정보 검색 완료: 총 {} 청크 중 {} 개 반환", 
            publicChunks.size(), topChunks.size());
        
        return SearchResult.builder()
            .chunks(topChunks)
            .contextText(contextText)
            .totalChunks(publicChunks.size())
            .build();
    }
    
    /**
     * 공개 정보만 대상으로 키워드 검색 수행
     */
    private SearchResult performPublicKeywordSearch(String query, List<Chunk> publicChunks, int topK) {
        List<SearchResult.ScoredChunk> keywordMatches = new ArrayList<>();
        
        String normalizedQuery = query.trim();
        String[] keywords = normalizedQuery.split("\\s+");
        
        for (Chunk chunk : publicChunks) {
            String chunkText = chunk.getText();
            double score = 0.0;
            
            // 키워드별 점수 계산 (기존 로직 재사용)
            for (String keyword : keywords) {
                if (keyword.length() < 1) continue;
                
                if (containsIgnoreCase(chunkText, keyword)) {
                    score += 10.0;
                    if (isInImportantField(chunkText, keyword)) {
                        score += 20.0;
                    }
                }
                
                if (keyword.length() >= 2 && isKorean(keyword)) {
                    for (int i = 0; i <= keyword.length() - 2; i++) {
                        String partial = keyword.substring(i, Math.min(i + 2, keyword.length()));
                        if (containsIgnoreCase(chunkText, partial)) {
                            score += 2.0;
                        }
                    }
                }
                
                if (keyword.length() >= 3 && isEnglish(keyword)) {
                    for (int i = 0; i <= keyword.length() - 3; i++) {
                        String partial = keyword.substring(i, Math.min(i + 3, keyword.length()));
                        if (containsIgnoreCase(chunkText, partial)) {
                            score += 1.0;
                        }
                    }
                }
            }
            
            if (containsIgnoreCase(chunkText, normalizedQuery)) {
                score += 50.0;
            }
            
            if (score > 0) {
                double normalizedScore = Math.min(score / keywords.length, 100.0);
                keywordMatches.add(SearchResult.ScoredChunk.builder()
                    .chunk(chunk)
                    .similarity(normalizedScore)
                    .build());
            }
        }
        
        List<SearchResult.ScoredChunk> topKeywordChunks = keywordMatches.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());
        
        return SearchResult.builder()
            .chunks(topKeywordChunks)
            .contextText(buildContextText(topKeywordChunks))
            .totalChunks(publicChunks.size())
            .build();
    }
    
    /**
     * 공개 정보 벡터 검색과 키워드 검색 결과 결합
     */
    private SearchResult combinePublicSearchResults(List<SearchResult.ScoredChunk> vectorChunks, 
                                                   SearchResult keywordResult, int topK, String query) {
        Set<String> seenChunkIds = new HashSet<>();
        List<SearchResult.ScoredChunk> combinedChunks = new ArrayList<>();
        
        boolean isKoreanQuery = containsKorean(query);
        
        if (isKoreanQuery && !keywordResult.getChunks().isEmpty()) {
            // 한글 쿼리: 키워드 검색 우선
            for (SearchResult.ScoredChunk chunk : keywordResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId())) {
                    SearchResult.ScoredChunk boostedChunk = SearchResult.ScoredChunk.builder()
                        .chunk(chunk.getChunk())
                        .similarity(Math.min(chunk.getSimilarity() * 1.5, 1.0))
                        .build();
                    combinedChunks.add(boostedChunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
            
            // 벡터 검색 결과 추가
            for (SearchResult.ScoredChunk chunk : vectorChunks) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId()) && combinedChunks.size() < topK) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
        } else {
            // 영어 쿼리: 벡터 검색 우선
            for (SearchResult.ScoredChunk chunk : vectorChunks) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId())) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
            
            for (SearchResult.ScoredChunk chunk : keywordResult.getChunks()) {
                if (!seenChunkIds.contains(chunk.getChunk().getChunkId()) && combinedChunks.size() < topK) {
                    combinedChunks.add(chunk);
                    seenChunkIds.add(chunk.getChunk().getChunkId());
                }
            }
        }
        
        combinedChunks = combinedChunks.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());
        
        return SearchResult.builder()
            .chunks(combinedChunks)
            .contextText(buildContextText(combinedChunks))
            .totalChunks(keywordResult.getTotalChunks())
            .build();
    }
    
    /**
     * 캐시 상태 확인
     */
    public Map<String, Object> getCacheStatus() {
        return Map.of(
            "initialized", cacheInitialized,
            "size", chunkCache.size(),
            "totalInRedis", repository.getTotalChunkCount()
        );
    }
}