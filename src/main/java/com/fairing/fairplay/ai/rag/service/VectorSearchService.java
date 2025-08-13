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
    
    // 검색 설정
    private static final int DEFAULT_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.25;
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
        
        if (chunkCache.isEmpty()) {
            return SearchResult.builder()
                .chunks(Collections.emptyList())
                .contextText("검색할 수 있는 문서가 없습니다.")
                .totalChunks(0)
                .build();
        }
        
        // 질의 임베딩
        float[] queryEmbedding = embeddingService.embedText(query);
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
            
            // 컨텍스트 구분자 추가
            String chunkWithHeader = String.format("[컨텍스트 %d] %s\n\n", i + 1, chunkText);
            
            // 최대 길이 초과 확인
            if (currentLength + chunkWithHeader.length() > MAX_CONTEXT_LENGTH) {
                if (i == 0) {
                    // 첫 번째 청크라도 너무 길면 잘라서 포함
                    int remainingLength = MAX_CONTEXT_LENGTH - String.format("[컨텍스트 %d] ", i + 1).length() - 20;
                    if (remainingLength > 100) {
                        String truncated = chunkText.length() > remainingLength ? 
                            chunkText.substring(0, remainingLength) + "..." : chunkText;
                        context.append(String.format("[컨텍스트 %d] %s\n\n", i + 1, truncated));
                    }
                }
                break;
            }
            
            context.append(chunkWithHeader);
            currentLength += chunkWithHeader.length();
        }
        
        return context.toString().trim();
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