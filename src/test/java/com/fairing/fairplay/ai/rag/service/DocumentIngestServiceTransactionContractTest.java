package com.fairing.fairplay.ai.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIngestServiceTransactionContractTest {

    @Test
    void ingestDocumentUsesIndependentWriteTransaction() throws Exception {
        Method method = DocumentIngestService.class.getMethod(
            "ingestDocument",
            com.fairing.fairplay.ai.rag.domain.Document.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void deleteDocumentUsesIndependentWriteTransaction() throws Exception {
        Method method = DocumentIngestService.class.getMethod("deleteDocument", String.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
