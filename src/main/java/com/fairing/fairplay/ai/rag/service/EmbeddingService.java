package com.fairing.fairplay.ai.rag.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI 임베딩 서비스 (REST API 직접 호출)
 * embedding-001 모델 사용
 */
@Service
@Slf4j
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    
    private static final int VECTOR_DIMENSION = 768; // Gemini embedding-001 차원
    private static final String GEMINI_EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent";

    public EmbeddingService(@Value("${gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * 텍스트를 Gemini embedding-001로 임베딩 벡터 생성
     */
    public float[] embedText(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다.");
        }

        try {
            String cleanText = preprocessText(text);
            log.debug("Gemini 임베딩 요청: {} 문자", cleanText.length());

            // 요청 바디 구성
            Map<String, Object> requestBody = Map.of(
                "content", Map.of(
                    "parts", List.of(Map.of("text", cleanText))
                ),
                "taskType", "RETRIEVAL_DOCUMENT"
            );

            // Gemini API 호출
            GeminiEmbeddingResponse response = webClient
                    .post()
                    .uri(GEMINI_EMBEDDING_URL + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeminiEmbeddingResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.embedding == null || response.embedding.values == null) {
                throw new RuntimeException("Gemini API 응답이 비어있습니다");
            }

            // List<Double>을 float[]로 변환
            List<Double> values = response.embedding.values;
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = values.get(i).floatValue();
            }

            log.debug("Gemini 임베딩 완료: {} 차원 벡터 생성", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Gemini 임베딩 실패: {}", e.getMessage(), e);
            
            // Fallback: 간단한 해시 기반 벡터 (오류 시에만)
            log.warn("Gemini 임베딩 실패로 해시 기반 벡터로 대체");
            return generateFallbackVector(text);
        }
    }

    /**
     * 질의용 임베딩 생성 (task_type이 다름)
     */
    public float[] embedQuery(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("질의가 비어있습니다.");
        }

        try {
            String cleanQuery = preprocessText(query);
            log.debug("Gemini 질의 임베딩 요청: {} 문자", cleanQuery.length());

            // 요청 바디 구성 (RETRIEVAL_QUERY 타입)
            Map<String, Object> requestBody = Map.of(
                "content", Map.of(
                    "parts", List.of(Map.of("text", cleanQuery))
                ),
                "taskType", "RETRIEVAL_QUERY"
            );

            // Gemini API 호출
            GeminiEmbeddingResponse response = webClient
                    .post()
                    .uri(GEMINI_EMBEDDING_URL + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeminiEmbeddingResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.embedding == null || response.embedding.values == null) {
                throw new RuntimeException("Gemini API 응답이 비어있습니다");
            }

            List<Double> values = response.embedding.values;
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = values.get(i).floatValue();
            }

            log.debug("Gemini 질의 임베딩 완료: {} 차원", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Gemini 질의 임베딩 실패, 일반 임베딩으로 대체: {}", e.getMessage(), e);
            return embedText(query);
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

    /**
     * Gemini 실패 시 폴백 벡터 생성
     */
    private float[] generateFallbackVector(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            float[] vector = new float[VECTOR_DIMENSION];
            
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
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
            return new float[VECTOR_DIMENSION]; // 영벡터 반환
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