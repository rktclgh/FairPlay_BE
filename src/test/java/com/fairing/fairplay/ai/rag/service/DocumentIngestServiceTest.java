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

        when(chunkingService.chunkDocument(document))
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

        when(chunkingService.chunkDocument(document))
            .thenReturn(List.of(chunk));
        when(embeddingService.embedText(chunk.getText()))
            .thenThrow(new IllegalStateException("embedding unavailable"));

        DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);

        assertThat(result.isSuccess()).isFalse();
        verify(ragChunkWriteService, never()).replaceDocument(anyString(), anyList());
        verify(vectorSearchService, never()).invalidateCache();
    }

    @Test
    void propagatesDocumentScopeMetadataToWrittenChunks() throws Exception {
        Document document = Document.builder()
            .docId("reservation_141")
            .title("2025 트렌드페어 예약")
            .content("2025 트렌드페어 개인 예약 내역입니다.")
            .category("user_reservation")
            .docType("USER_RESERVATION")
            .visibility("USER_PRIVATE")
            .ownerUserId(1081L)
            .eventId(52L)
            .reservationId(141L)
            .build();
        Chunk chunk = Chunk.builder()
            .chunkId("chunk_1")
            .docId("reservation_141")
            .text("2025 트렌드페어 개인 예약 내역입니다.")
            .docType("USER_RESERVATION")
            .visibility("USER_PRIVATE")
            .ownerUserId(1081L)
            .eventId(52L)
            .reservationId(141L)
            .build();

        when(chunkingService.chunkDocument(document)).thenReturn(List.of(chunk));
        when(embeddingService.embedText(chunk.getText())).thenReturn(new float[] {0.1f, 0.2f});

        documentIngestService.ingestDocument(document);

        verify(ragChunkWriteService).replaceDocument(eq("reservation_141"), argThat(chunks ->
            chunks.size() == 1
                && "USER_RESERVATION".equals(chunks.get(0).getDocType())
                && "USER_PRIVATE".equals(chunks.get(0).getVisibility())
                && Long.valueOf(1081L).equals(chunks.get(0).getOwnerUserId())
                && Long.valueOf(52L).equals(chunks.get(0).getEventId())
                && Long.valueOf(141L).equals(chunks.get(0).getReservationId())
        ));
    }

    private Document document() {
        return Document.builder()
            .docId("event_52")
            .title("2025 트렌드페어")
            .content("2025 트렌드페어 행사 정보와 장소, 기간, 티켓 정보를 포함한 충분히 긴 테스트 문서입니다.")
            .category("event")
            .docType("PUBLIC_EVENT")
            .visibility("PUBLIC")
            .eventId(52L)
            .build();
    }

    private Chunk chunk() {
        return Chunk.builder()
            .chunkId("chunk_1")
            .docId("event_52")
            .text("2025 트렌드페어 행사 정보와 장소, 기간, 티켓 정보를 포함한 청크입니다.")
            .docType("PUBLIC_EVENT")
            .visibility("PUBLIC")
            .eventId(52L)
            .build();
    }
}
