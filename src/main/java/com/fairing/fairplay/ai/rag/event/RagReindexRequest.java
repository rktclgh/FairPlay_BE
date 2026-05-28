package com.fairing.fairplay.ai.rag.event;

public record RagReindexRequest(
    RagDocumentType documentType,
    Long documentId,
    RagReindexAction action
) {
    public static RagReindexRequest upsert(RagDocumentType documentType, Long documentId) {
        return new RagReindexRequest(documentType, documentId, RagReindexAction.UPSERT);
    }

    public static RagReindexRequest delete(RagDocumentType documentType, Long documentId) {
        return new RagReindexRequest(documentType, documentId, RagReindexAction.DELETE);
    }
}
