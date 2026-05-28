package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestServiceTest {

    @Mock
    ChunkingService chunkingService;

    @Mock
    EmbeddingService embeddingService;

    @Mock
    RagChunkWriteService ragChunkWriteService;

    @Mock
    VectorSearchService vectorSearchService;

    ThreadPoolTaskExecutor taskExecutor;
    DocumentIngestService documentIngestService;

    @BeforeEach
    void setUp() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.initialize();

        documentIngestService = new DocumentIngestService(
            chunkingService,
            embeddingService,
            ragChunkWriteService,
            vectorSearchService,
            taskExecutor
        );
    }

    @AfterEach
    void tearDown() {
        taskExecutor.shutdown();
    }

    @Test
    void replacesDocumentAfterEmbeddingSucceeds() throws Exception {
        Document document = document();
        Chunk chunk = chunk();

        when(chunkingService.chunkDocument(document.getDocId(), document.getContent()))
            .thenReturn(List.of(chunk));
        when(embeddingService.embedText(chunk.getText()))
            .thenReturn(new float[] {0.1f, 0.2f});

        DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);

        assertThat(result.isSuccess()).isTrue();
        verify(ragChunkWriteService).replaceDocument(eq(document.getDocId()), argThat(chunks ->
            chunks.size() == 1 && chunks.get(0).getEmbedding().length == 2
        ));
        verify(vectorSearchService).invalidateCache();
    }

    @Test
    void keepsExistingDocumentWhenEveryEmbeddingFails() throws Exception {
        Document document = document();
        Chunk chunk = chunk();

        when(chunkingService.chunkDocument(document.getDocId(), document.getContent()))
            .thenReturn(List.of(chunk));
        when(embeddingService.embedText(chunk.getText()))
            .thenThrow(new IllegalStateException("embedding unavailable"));

        DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);

        assertThat(result.isSuccess()).isFalse();
        verify(ragChunkWriteService, never()).replaceDocument(anyString(), anyList());
        verify(vectorSearchService, never()).invalidateCache();
    }

    private Document document() {
        return Document.builder()
            .docId("event_52")
            .title("2025 트렌드페어")
            .content("2025 트렌드페어 행사 정보와 장소, 기간, 티켓 정보를 포함한 충분히 긴 테스트 문서입니다.")
            .category("event")
            .build();
    }

    private Chunk chunk() {
        return Chunk.builder()
            .chunkId("chunk_1")
            .docId("event_52")
            .text("2025 트렌드페어 행사 정보와 장소, 기간, 티켓 정보를 포함한 청크입니다.")
            .build();
    }
}
