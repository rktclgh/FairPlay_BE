package com.fairing.fairplay.ai.rag.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 검색 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private List<ScoredChunk> chunks;
    private String contextText;
    private int totalChunks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoredChunk {
        private Chunk chunk;
        private double similarity;
    }
}