package com.fairing.fairplay.ai.rag.event;

import java.util.Objects;

public record RagReindexRequest(
    RagDocumentType documentType,
    Long documentId,
    RagReindexAction action
) {
    public RagReindexRequest {
        Objects.requireNonNull(documentType, "documentType must not be null");
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (documentId <= 0) {
            throw new IllegalArgumentException("documentId must be positive");
        }
    }

    public static RagReindexRequest upsert(RagDocumentType documentType, Long documentId) {
        return new RagReindexRequest(documentType, documentId, RagReindexAction.UPSERT);
    }

    public static RagReindexRequest delete(RagDocumentType documentType, Long documentId) {
        return new RagReindexRequest(documentType, documentId, RagReindexAction.DELETE);
    }
}
