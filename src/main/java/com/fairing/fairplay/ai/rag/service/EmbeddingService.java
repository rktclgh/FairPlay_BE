package com.fairing.fairplay.ai.rag.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI 임베딩 서비스 (REST API 직접 호출)
 * gemini-embedding-001 모델 사용
 */
@Service
@Slf4j
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiKey;
    private final boolean fallbackEnabled;
    private final String embeddingModel;
    private final int outputDimensionality;
    private final int maxRetries;
    private final long retryBaseDelayMillis;
    private final long minRequestIntervalMillis;
    private final Object throttleLock = new Object();
    private long nextRequestAtMillis;
    
    private static final int DEFAULT_VECTOR_DIMENSION = 768;
    private static final String DEFAULT_EMBEDDING_MODEL = "gemini-embedding-001";
    private static final String GEMINI_EMBEDDING_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent";

    public EmbeddingService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${rag.embedding.fallback-enabled:false}") boolean fallbackEnabled,
            @Value("${rag.embedding.model:gemini-embedding-001}") String embeddingModel,
            @Value("${rag.embedding.output-dimensionality:768}") int outputDimensionality,
            @Value("${rag.embedding.max-retries:5}") int maxRetries,
            @Value("${rag.embedding.retry-base-delay-ms:1000}") long retryBaseDelayMillis,
            @Value("${rag.embedding.min-request-interval-ms:1200}") long minRequestIntervalMillis) {
        this.apiKey = apiKey;
        this.fallbackEnabled = fallbackEnabled;
        this.embeddingModel = normalizeEmbeddingModel(embeddingModel);
        this.outputDimensionality = outputDimensionality > 0 ? outputDimensionality : DEFAULT_VECTOR_DIMENSION;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBaseDelayMillis = Math.max(100, retryBaseDelayMillis);
        this.minRequestIntervalMillis = Math.max(0, minRequestIntervalMillis);
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * 텍스트를 Gemini embedding 모델로 임베딩 벡터 생성
     */
    public float[] embedText(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return failOrFallback(text, "Gemini API key is empty.");
        }

        try {
            String cleanText = preprocessText(text);
            log.debug("Gemini 임베딩 요청: {} 문자", cleanText.length());

            Map<String, Object> requestBody = buildEmbeddingRequest(cleanText, "RETRIEVAL_DOCUMENT");
            float[] embedding = requestEmbedding(requestBody, "Gemini 임베딩");

            log.debug("Gemini 임베딩 완료: {} 차원 벡터 생성", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Gemini 임베딩 실패: {}", e.getMessage(), e);
            return failOrFallback(text, "Gemini embedding failed: " + e.getMessage());
        }
    }

    /**
     * 질의용 임베딩 생성 (task_type이 다름)
     */
    public float[] embedQuery(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("질의가 비어있습니다.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return failOrFallback(query, "Gemini API key is empty.");
        }

        try {
            String cleanQuery = preprocessText(query);
            log.debug("Gemini 질의 임베딩 요청: {} 문자", cleanQuery.length());

            Map<String, Object> requestBody = buildEmbeddingRequest(cleanQuery, "RETRIEVAL_QUERY");
            float[] embedding = requestEmbedding(requestBody, "Gemini 질의 임베딩");

            log.debug("Gemini 질의 임베딩 완료: {} 차원", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Gemini 질의 임베딩 실패: {}", e.getMessage(), e);
            return failOrFallback(query, "Gemini query embedding failed: " + e.getMessage());
        }
    }

    /**
     * 텍스트 전처리
     */
    private String preprocessText(String text) {
        return text
            .replaceAll("\\s+", " ")
            .trim();
    }

    private float[] failOrFallback(String text, String message) {
        if (fallbackEnabled) {
            log.warn("{} Using deterministic fallback vector because rag.embedding.fallback-enabled=true.", message);
            return generateFallbackVector(text);
        }
        throw new IllegalStateException(message + " Refusing to store/search deterministic fallback vectors.");
    }

    String embeddingUrl() {
        return String.format(GEMINI_EMBEDDING_URL_TEMPLATE, embeddingModel);
    }

    Map<String, Object> buildEmbeddingRequest(String text, String taskType) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("content", Map.of(
            "parts", List.of(Map.of("text", preprocessText(text)))
        ));
        requestBody.put("taskType", taskType);
        requestBody.put("outputDimensionality", outputDimensionality);
        return requestBody;
    }

    long retryDelayMillis(Throwable failure, int attempt) {
        if (failure instanceof WebClientResponseException responseException) {
            String retryAfter = responseException.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
            Long retryAfterMillis = parseRetryAfterMillis(retryAfter);
            if (retryAfterMillis != null) {
                return retryAfterMillis;
            }
        }
        return Math.min(30_000L, retryBaseDelayMillis * (1L << Math.min(attempt, 5)));
    }

    private float[] requestEmbedding(Map<String, Object> requestBody, String operationName) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                throttleBeforeRequest();
                GeminiEmbeddingResponse response = webClient
                        .post()
                        .uri(embeddingUrl())
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(GeminiEmbeddingResponse.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                return toFloatArray(response);
            } catch (Exception e) {
                if (!isRetryable(e) || attempt >= maxRetries) {
                    throw e;
                }
                long delayMillis = retryDelayMillis(e, attempt);
                log.warn("{} 재시도 대기: attempt={}, delay={}ms, cause={}",
                    operationName, attempt + 1, delayMillis, e.getMessage());
                sleep(delayMillis);
            }
        }
    }

    private float[] toFloatArray(GeminiEmbeddingResponse response) {
        if (response == null || response.embedding == null || response.embedding.values == null) {
            throw new RuntimeException("Gemini API 응답이 비어있습니다");
        }

        List<Double> values = response.embedding.values;
        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embedding[i] = values.get(i).floatValue();
        }
        return embedding;
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof WebClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
        }
        return false;
    }

    private void throttleBeforeRequest() {
        if (minRequestIntervalMillis <= 0) {
            return;
        }
        synchronized (throttleLock) {
            long now = System.currentTimeMillis();
            if (nextRequestAtMillis > now) {
                sleep(nextRequestAtMillis - now);
                now = System.currentTimeMillis();
            }
            nextRequestAtMillis = now + minRequestIntervalMillis;
        }
    }

    private Long parseRetryAfterMillis(String retryAfter) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }
        try {
            return Math.max(0L, Long.parseLong(retryAfter.trim()) * 1000L);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini embedding request interrupted", interruptedException);
        }
    }

    private String normalizeEmbeddingModel(String configuredModel) {
        if (configuredModel == null || configuredModel.isBlank()) {
            return DEFAULT_EMBEDDING_MODEL;
        }
        return configuredModel.trim().replaceFirst("^models/", "");
    }

    /**
     * Gemini 실패 시 폴백 벡터 생성
     */
    private float[] generateFallbackVector(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            float[] vector = new float[outputDimensionality];
            
            for (int i = 0; i < outputDimensionality; i++) {
                int hashIndex = i % hash.length;
                vector[i] = ((hash[hashIndex] & 0xFF) - 127.5f) / 127.5f;
            }
            
            // 텍스트의 특성을 반영한 간단한 특징 추가
            String[] words = text.split("\\s+");
            
            // 특정 키워드에 따른 벡터 조정 (이벤트 관련)
            if (text.contains("이벤트") || text.contains("event")) {
                for (int i = 0; i < 50; i++) vector[i] += 0.3f;
            }
            if (text.contains("예약") || text.contains("reservation")) {
                for (int i = 50; i < 100; i++) vector[i] += 0.3f;
            }
            if (text.contains("티켓") || text.contains("ticket")) {
                for (int i = 100; i < 150; i++) vector[i] += 0.3f;
            }
            if (text.contains("장소") || text.contains("위치") || text.contains("location")) {
                for (int i = 150; i < 200; i++) vector[i] += 0.3f;
            }
            
            // 벡터 정규화
            float norm = 0.0f;
            for (float v : vector) norm += v * v;
            norm = (float) Math.sqrt(norm);
            
            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }
            
            return vector;
        } catch (Exception e) {
            log.error("폴백 벡터 생성도 실패: {}", e.getMessage(), e);
            return new float[outputDimensionality]; // 영벡터 반환
        }
    }

    /**
     * 코사인 유사도 계산 (차원 호환성 처리)
     */
    public double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        // 차원이 다른 경우 호환성 처리
        if (vectorA.length != vectorB.length) {
            log.warn("벡터 차원 불일치: {} vs {} - 호환성 처리 진행", vectorA.length, vectorB.length);
            
            // 더 작은 차원으로 맞춤
            int minDim = Math.min(vectorA.length, vectorB.length);
            float[] alignedA = new float[minDim];
            float[] alignedB = new float[minDim];
            
            System.arraycopy(vectorA, 0, alignedA, 0, minDim);
            System.arraycopy(vectorB, 0, alignedB, 0, minDim);
            
            return calculateCosineSimilarityInternal(alignedA, alignedB);
        }
        
        return calculateCosineSimilarityInternal(vectorA, vectorB);
    }

    /**
     * 내부 코사인 유사도 계산 (동일 차원 보장)
     */
    private double calculateCosineSimilarityInternal(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0; // 하나의 벡터가 영벡터인 경우
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Gemini API 응답을 위한 DTO 클래스들
     */
    public static class GeminiEmbeddingResponse {
        @JsonProperty("embedding")
        public EmbeddingData embedding;
        
        public static class EmbeddingData {
            @JsonProperty("values")
            public List<Double> values;
        }
    }
}
