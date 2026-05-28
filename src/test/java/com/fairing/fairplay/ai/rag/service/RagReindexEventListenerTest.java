package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.event.RagDocumentType;
import com.fairing.fairplay.ai.rag.event.RagReindexRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RagReindexEventListenerTest {

    @Mock
    ComprehensiveRagDataLoader comprehensiveRagDataLoader;

    @Mock
    DocumentIngestService documentIngestService;

    @InjectMocks
    RagReindexEventListener listener;

    @Test
    void eventUpsertReloadsSingleEventDocument() {
        listener.handle(RagReindexRequest.upsert(RagDocumentType.EVENT, 52L));

        verify(comprehensiveRagDataLoader).loadSingleEvent(52L);
    }

    @Test
    void boothUpsertReloadsSingleBoothDocument() {
        listener.handle(RagReindexRequest.upsert(RagDocumentType.BOOTH, 11L));

        verify(comprehensiveRagDataLoader).loadSingleBooth(11L);
    }

    @Test
    void boothExperienceUpsertReloadsSingleExperienceDocument() {
        listener.handle(RagReindexRequest.upsert(RagDocumentType.BOOTH_EXPERIENCE, 7L));

        verify(comprehensiveRagDataLoader).loadSingleBoothExperience(7L);
    }

    @Test
    void deleteRemovesStoredDocument() {
        listener.handle(RagReindexRequest.delete(RagDocumentType.EVENT, 52L));

        verify(documentIngestService).deleteDocument("event_52");
    }
}
