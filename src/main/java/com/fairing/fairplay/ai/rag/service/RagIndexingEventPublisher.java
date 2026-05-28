package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.event.RagDocumentType;
import com.fairing.fairplay.ai.rag.event.RagReindexRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagIndexingEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void eventChanged(Long eventId) {
        publishUpsert(RagDocumentType.EVENT, eventId);
    }

    public void eventDeleted(Long eventId) {
        publishDelete(RagDocumentType.EVENT, eventId);
    }

    public void boothChanged(Long boothId) {
        publishUpsert(RagDocumentType.BOOTH, boothId);
    }

    public void boothDeleted(Long boothId) {
        publishDelete(RagDocumentType.BOOTH, boothId);
    }

    public void boothExperienceChanged(Long experienceId) {
        publishUpsert(RagDocumentType.BOOTH_EXPERIENCE, experienceId);
    }

    public void boothExperienceDeleted(Long experienceId) {
        publishDelete(RagDocumentType.BOOTH_EXPERIENCE, experienceId);
    }

    public void userDataChanged(Long userId) {
        publishUpsert(RagDocumentType.USER_DATA, userId);
    }

    public void userDataDeleted(Long userId) {
        publishDelete(RagDocumentType.USER_DATA, userId);
    }

    private void publishUpsert(RagDocumentType documentType, Long documentId) {
        if (documentId != null) {
            eventPublisher.publishEvent(RagReindexRequest.upsert(documentType, documentId));
        }
    }

    private void publishDelete(RagDocumentType documentType, Long documentId) {
        if (documentId != null) {
            eventPublisher.publishEvent(RagReindexRequest.delete(documentType, documentId));
        }
    }
}
