package com.fairing.fairplay.ai.rag.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 청크 도메인 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private String chunkId;
    private String docId;
    private String text;
    private float[] embedding;
    private long createdAt;
}