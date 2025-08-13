package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.client.LlmClient;
import com.fairing.fairplay.ai.service.LlmRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 임베딩 서비스
 * OpenAI 임베딩 API 또는 내부 임베더 사용
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final LlmRouter llmRouter;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // OpenAI 임베딩 설정
    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small"; // 1536 차원, 저렴함
    
    /**
     * 텍스트를 임베딩 벡터로 변환
     */
    public float[] embedText(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다.");
        }
        
        String cleanText = preprocessText(text);
        return callOpenAiEmbedding(cleanText);
    }
    
    /**
     * 텍스트 전처리
     */
    private String preprocessText(String text) {
        return text
            .toLowerCase()
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * OpenAI 임베딩 API 호출
     */
    private float[] callOpenAiEmbedding(String text) throws Exception {
        // OpenAI 클라이언트에서 API 키 가져오기
        LlmClient openAiClient = llmRouter.pick("OPENAI");
        
        // OpenAI API 키를 리플렉션으로 가져오기 (임시 방법)
        String apiKey = getOpenAiApiKey();
        
        Map<String, Object> requestBody = Map.of(
            "model", EMBEDDING_MODEL,
            "input", text
        );
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_EMBEDDING_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI 임베딩 API 오류: HTTP " + response.statusCode() + " - " + response.body());
        }
        
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dataArray = root.path("data");
        
        if (!dataArray.isArray() || dataArray.isEmpty()) {
            throw new RuntimeException("임베딩 응답에 데이터가 없습니다.");
        }
        
        JsonNode embedding = dataArray.get(0).path("embedding");
        if (!embedding.isArray()) {
            throw new RuntimeException("임베딩 벡터가 배열 형태가 아닙니다.");
        }
        
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = (float) embedding.get(i).asDouble();
        }
        
        return vector;
    }
    
    /**
     * OpenAI API 키 가져오기
     * 실제로는 환경변수나 설정에서 가져와야 함
     */
    private String getOpenAiApiKey() {
        // 환경변수에서 가져오기
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        return apiKey;
    }
    
    /**
     * 코사인 유사도 계산
     */
    public double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("벡터 길이가 다릅니다: " + vectorA.length + " vs " + vectorB.length);
        }
        
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
}