package com.fairing.fairplay.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.SearchResult;
import com.fairing.fairplay.ai.rag.repository.RagChunkRepository;

class VectorSearchServiceScopeTest {

    private RagChunkRepository repository;
    private EmbeddingService embeddingService;
    private VectorSearchService service;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(RagChunkRepository.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        service = new VectorSearchService(repository, embeddingService);

        when(embeddingService.embedQuery(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
    }

    @Test
    void userDataSearchDoesNotRequireLegacyUserDocIdBeforeSearchingOwnerScopedChunks() throws Exception {
        Chunk reservationChunk = Chunk.builder()
            .chunkId("chunk_reservation_24")
            .docId("reservation_24")
            .text("=== 개인 예약 내역 ===\n행사명: 2025 명원세계차(茶) 품평대회")
            .build();
        SearchResult.ScoredChunk scoredChunk = SearchResult.ScoredChunk.builder()
            .chunk(reservationChunk)
            .similarity(0.9)
            .build();
        when(repository.searchUserSimilarByTypes(eq(10L), anyList(), any(), anyInt(), anyDouble()))
            .thenReturn(List.of(scoredChunk));
        when(repository.searchUserKeywordByTypes(eq(10L), anyList(), any(), anyInt()))
            .thenReturn(List.of());

        SearchResult result = service.searchUserData(10L, "내 예약 알려줘");

        assertThat(result.getChunks()).containsExactly(scoredChunk);
        assertThat(result.getContextText()).contains("2025 명원세계차");
        verify(repository, never()).findByDocId("user_10");
    }
}
