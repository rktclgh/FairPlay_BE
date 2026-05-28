package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.event.RagReindexAction;
import com.fairing.fairplay.ai.rag.event.RagReindexRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagReindexEventListener {

    private final ComprehensiveRagDataLoader comprehensiveRagDataLoader;
    private final DocumentIngestService documentIngestService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RagReindexRequest request) {
        try {
            if (request.action() == RagReindexAction.DELETE) {
                deleteDocument(request);
                return;
            }

            switch (request.documentType()) {
                case EVENT -> comprehensiveRagDataLoader.loadSingleEvent(request.documentId());
                case BOOTH -> comprehensiveRagDataLoader.loadSingleBooth(request.documentId());
                case BOOTH_EXPERIENCE -> comprehensiveRagDataLoader.loadSingleBoothExperience(request.documentId());
            }
        } catch (Exception e) {
            log.error("RAG reindex event failed. type={}, id={}, action={}",
                request.documentType(), request.documentId(), request.action(), e);
            throw new IllegalStateException("RAG reindex event failed", e);
        }
    }

    private void deleteDocument(RagReindexRequest request) {
        String docId = switch (request.documentType()) {
            case EVENT -> "event_" + request.documentId();
            case BOOTH -> "booth_" + request.documentId();
            case BOOTH_EXPERIENCE -> "booth_experience_" + request.documentId();
        };
        documentIngestService.deleteDocument(docId);
    }
}
