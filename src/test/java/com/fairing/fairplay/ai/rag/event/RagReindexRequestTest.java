package com.fairing.fairplay.ai.rag.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagReindexRequestTest {

    @Test
    void rejectsNullDocumentType() {
        assertThatThrownBy(() -> RagReindexRequest.upsert(null, 1L))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("documentType must not be null");
    }

    @Test
    void rejectsNullDocumentId() {
        assertThatThrownBy(() -> RagReindexRequest.delete(RagDocumentType.EVENT, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("documentId must not be null");
    }

    @Test
    void rejectsNonPositiveDocumentId() {
        assertThatThrownBy(() -> RagReindexRequest.upsert(RagDocumentType.EVENT, 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("documentId must be positive");
    }
}
