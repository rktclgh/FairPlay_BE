package com.fairing.fairplay.ai.rag.repository;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.SearchResult;

import java.util.List;

public interface RagChunkRepository {

    void saveChunks(List<Chunk> chunks);

    List<SearchResult.ScoredChunk> searchSimilar(float[] queryEmbedding, int topK, double threshold);

    List<SearchResult.ScoredChunk> searchPublicSimilar(float[] queryEmbedding, int topK, double threshold);

    List<SearchResult.ScoredChunk> searchUserSimilar(Long userId, float[] queryEmbedding, int topK, double threshold);

    List<SearchResult.ScoredChunk> searchKeyword(String query, int topK);

    List<SearchResult.ScoredChunk> searchPublicKeyword(String query, int topK);

    List<SearchResult.ScoredChunk> searchUserKeyword(Long userId, String query, int topK);

    List<Chunk> findByDocId(String docId);

    void deleteDocument(String docId);

    void clearAllData();

    long getTotalChunkCount();
}
