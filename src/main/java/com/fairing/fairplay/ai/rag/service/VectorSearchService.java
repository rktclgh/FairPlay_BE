package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.SearchResult;
import com.fairing.fairplay.ai.rag.repository.RagChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.1;
    private static final double USER_SIMILARITY_THRESHOLD = 0.05;
    private static final int MAX_CONTEXT_LENGTH = 2000;

    private final RagChunkRepository repository;
    private final EmbeddingService embeddingService;

    public SearchResult search(String query, int topK) throws Exception {
        if (isBlank(query)) {
            return emptyResult("");
        }

        float[] queryEmbedding = embeddingService.embedQuery(query);
        List<SearchResult.ScoredChunk> vectorChunks =
            repository.searchSimilar(queryEmbedding, topK, SIMILARITY_THRESHOLD);
        List<SearchResult.ScoredChunk> keywordChunks =
            shouldAddKeywordSearch(query, vectorChunks, topK)
                ? repository.searchKeyword(query, topK)
                : List.of();

        return toResult(combineSearchResults(vectorChunks, keywordChunks, topK, query));
    }

    public SearchResult search(String query) throws Exception {
        return search(query, DEFAULT_TOP_K);
    }

    public SearchResult searchUserData(Long userId, String query) throws Exception {
        if (userId == null || isBlank(query)) {
            return emptyResult("");
        }

        List<Chunk> userChunks = repository.findByDocId(userDocId(userId));
        if (userChunks.isEmpty()) {
            return emptyResult("해당 사용자의 정보를 찾을 수 없습니다.");
        }

        float[] queryEmbedding = embeddingService.embedQuery(query);
        List<SearchResult.ScoredChunk> vectorChunks =
            repository.searchUserSimilar(userId, queryEmbedding, DEFAULT_TOP_K, USER_SIMILARITY_THRESHOLD);
        List<SearchResult.ScoredChunk> keywordChunks =
            shouldAddKeywordSearch(query, vectorChunks, DEFAULT_TOP_K)
                ? repository.searchUserKeyword(userId, query, DEFAULT_TOP_K)
                : List.of();

        List<SearchResult.ScoredChunk> combined =
            combineSearchResults(vectorChunks, keywordChunks, DEFAULT_TOP_K, query);

        return SearchResult.builder()
            .chunks(combined)
            .contextText(buildContextTextFromScored(combined))
            .totalChunks(userChunks.size())
            .build();
    }

    public SearchResult searchPublicOnly(String query) throws Exception {
        if (isBlank(query)) {
            return emptyResult("");
        }

        float[] queryEmbedding = embeddingService.embedQuery(query);
        List<SearchResult.ScoredChunk> vectorChunks =
            repository.searchPublicSimilar(queryEmbedding, DEFAULT_TOP_K, SIMILARITY_THRESHOLD);
        List<SearchResult.ScoredChunk> keywordChunks =
            shouldAddKeywordSearch(query, vectorChunks, DEFAULT_TOP_K)
                ? repository.searchPublicKeyword(query, DEFAULT_TOP_K)
                : List.of();

        SearchResult result = toResult(
            combineSearchResults(vectorChunks, keywordChunks, DEFAULT_TOP_K, query)
        );

        if (result.getChunks().isEmpty()) {
            result.setContextText("검색할 수 있는 공개 정보가 없습니다.");
        }
        return result;
    }

    /**
     * pgvector now owns the search index. This method remains for old admin endpoints.
     */
    public void initializeCache() {
        log.info("RAG search index is managed by PostgreSQL pgvector. No JVM cache warmup is required.");
    }

    /**
     * pgvector searches current table contents directly, so cache invalidation is a no-op.
     */
    public void invalidateCache() {
        log.debug("RAG cache invalidation skipped because pgvector search has no JVM cache.");
    }

    public void clearAllEmbeddingData() {
        repository.clearAllData();
        log.info("모든 pgvector RAG 청크 삭제 완료 - 데이터 재생성 필요");
    }

    public Map<String, Object> getCacheStatus() {
        return Map.of(
            "initialized", true,
            "storage", "postgres-pgvector",
            "jvmCacheSize", 0,
            "totalInPostgres", repository.getTotalChunkCount()
        );
    }

    private boolean shouldAddKeywordSearch(
        String query,
        List<SearchResult.ScoredChunk> vectorChunks,
        int topK
    ) {
        if (containsKorean(query)) {
            return true;
        }
        if (vectorChunks.size() < Math.max(topK / 2, 1)) {
            return true;
        }
        return !vectorChunks.isEmpty() && vectorChunks.get(0).getSimilarity() < 0.5;
    }

    private List<SearchResult.ScoredChunk> combineSearchResults(
        List<SearchResult.ScoredChunk> vectorChunks,
        List<SearchResult.ScoredChunk> keywordChunks,
        int topK,
        String query
    ) {
        List<SearchResult.ScoredChunk> first = containsKorean(query) ? keywordChunks : vectorChunks;
        List<SearchResult.ScoredChunk> second = containsKorean(query) ? vectorChunks : keywordChunks;
        List<SearchResult.ScoredChunk> combined = new ArrayList<>();
        Set<String> seenChunkIds = new HashSet<>();

        addUnique(combined, seenChunkIds, first, containsKorean(query));
        addUnique(combined, seenChunkIds, second, false);

        return combined.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .toList();
    }

    private void addUnique(
        List<SearchResult.ScoredChunk> target,
        Set<String> seenChunkIds,
        List<SearchResult.ScoredChunk> source,
        boolean boostKeyword
    ) {
        for (SearchResult.ScoredChunk scoredChunk : source) {
            String chunkId = scoredChunk.getChunk().getChunkId();
            if (!seenChunkIds.add(chunkId)) {
                continue;
            }

            if (boostKeyword) {
                target.add(SearchResult.ScoredChunk.builder()
                    .chunk(scoredChunk.getChunk())
                    .similarity(Math.min(scoredChunk.getSimilarity() * 1.5, 1.0))
                    .build());
            } else {
                target.add(scoredChunk);
            }
        }
    }

    private SearchResult toResult(List<SearchResult.ScoredChunk> chunks) {
        return SearchResult.builder()
            .chunks(chunks)
            .contextText(buildContextTextFromScored(chunks))
            .totalChunks((int) repository.getTotalChunkCount())
            .build();
    }

    private SearchResult emptyResult(String contextText) {
        return SearchResult.builder()
            .chunks(Collections.emptyList())
            .contextText(contextText)
            .totalChunks(0)
            .build();
    }

    private String buildContextTextFromScored(List<SearchResult.ScoredChunk> scoredChunks) {
        return buildContextText(scoredChunks.stream()
            .map(SearchResult.ScoredChunk::getChunk)
            .toList());
    }

    private String buildContextText(List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        int currentLength = 0;

        for (Chunk chunk : chunks) {
            String chunkText = chunk.getText();
            String chunkWithSeparator = chunkText + "\n\n";

            if (currentLength + chunkWithSeparator.length() > MAX_CONTEXT_LENGTH) {
                if (currentLength == 0) {
                    int remainingLength = MAX_CONTEXT_LENGTH - 20;
                    if (remainingLength > 100) {
                        context.append(chunkText, 0, Math.min(chunkText.length(), remainingLength))
                            .append("...");
                    }
                }
                break;
            }

            context.append(chunkWithSeparator);
            currentLength += chunkWithSeparator.length();
        }

        return context.toString().trim();
    }

    private String userDocId(Long userId) {
        return "user_" + userId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean containsKorean(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }
}
