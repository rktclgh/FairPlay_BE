package com.fairing.fairplay.ai.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    @Test
    void usesCurrentGeminiEmbeddingModelEndpoint() {
        EmbeddingService embeddingService = new EmbeddingService(
            "test-key",
            false,
            "gemini-embedding-001",
            768,
            3,
            500,
            1100
        );

        assertThat(embeddingService.embeddingUrl())
            .isEqualTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent");
    }

    @Test
    void buildsRetrievalDocumentRequestWithConfiguredOutputDimension() {
        EmbeddingService embeddingService = new EmbeddingService(
            "test-key",
            false,
            "gemini-embedding-001",
            768,
            3,
            500,
            1100
        );

        Map<String, Object> requestBody = embeddingService.buildEmbeddingRequest(
            " 2025 트렌드페어\n행사 정보 ",
            "RETRIEVAL_DOCUMENT"
        );

        assertThat(requestBody)
            .containsEntry("taskType", "RETRIEVAL_DOCUMENT")
            .containsEntry("outputDimensionality", 768);
        assertThat(extractText(requestBody)).isEqualTo("2025 트렌드페어 행사 정보");
    }

    @Test
    void honorsRetryAfterHeaderForRateLimitedEmbeddingRequest() {
        EmbeddingService embeddingService = new EmbeddingService(
            "test-key",
            false,
            "gemini-embedding-001",
            768,
            3,
            500,
            1100
        );
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "2");
        WebClientResponseException rateLimit = WebClientResponseException.create(
            429,
            "Too Many Requests",
            headers,
            new byte[0],
            StandardCharsets.UTF_8
        );

        assertThat(embeddingService.retryDelayMillis(rateLimit, 0)).isEqualTo(2000);
    }

    @Test
    void usesExponentialRetryDelayWhenRetryAfterHeaderIsMissing() {
        EmbeddingService embeddingService = new EmbeddingService(
            "test-key",
            false,
            "gemini-embedding-001",
            768,
            3,
            500,
            1100
        );

        assertThat(embeddingService.retryDelayMillis(new RuntimeException("boom"), 0)).isEqualTo(500);
        assertThat(embeddingService.retryDelayMillis(new RuntimeException("boom"), 2)).isEqualTo(2000);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> requestBody) {
        Map<String, Object> content = (Map<String, Object>) requestBody.get("content");
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
        return parts.get(0).get("text");
    }
}
