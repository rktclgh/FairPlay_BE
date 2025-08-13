package com.fairing.fairplay.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 임베딩 서비스
 * 간단한 해시 기반 벡터 생성 (데모용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private static final int VECTOR_DIMENSION = 384; // 일반적인 임베딩 차원
    
    /**
     * 텍스트를 임베딩 벡터로 변환
     * 간단한 해시 기반 벡터 생성 (데모용)
     */
    public float[] embedText(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다.");
        }
        
        String cleanText = preprocessText(text);
        return generateHashBasedVector(cleanText);
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
     * 해시 기반 벡터 생성 (데모용)
     * 실제 운영에서는 Gemini나 다른 임베딩 모델 사용 필요
     */
    private float[] generateHashBasedVector(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
        
        float[] vector = new float[VECTOR_DIMENSION];
        
        // 해시를 기반으로 벡터 생성
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            int hashIndex = i % hash.length;
            vector[i] = ((hash[hashIndex] & 0xFF) - 127.5f) / 127.5f; // -1 ~ 1 범위로 정규화
        }
        
        // 텍스트의 특성을 반영한 간단한 특징 추가
        String[] words = text.split("\\s+");
        float wordCountFactor = Math.min(words.length / 10.0f, 1.0f);
        float lengthFactor = Math.min(text.length() / 100.0f, 1.0f);
        
        // 특정 키워드에 따른 벡터 조정 (이벤트 관련)
        if (text.contains("이벤트") || text.contains("event")) {
            for (int i = 0; i < 50; i++) vector[i] += 0.1f;
        }
        if (text.contains("예약") || text.contains("reservation")) {
            for (int i = 50; i < 100; i++) vector[i] += 0.1f;
        }
        if (text.contains("티켓") || text.contains("ticket")) {
            for (int i = 100; i < 150; i++) vector[i] += 0.1f;
        }
        if (text.contains("장소") || text.contains("위치") || text.contains("location")) {
            for (int i = 150; i < 200; i++) vector[i] += 0.1f;
        }
        
        // 벡터 정규화
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        
        log.debug("텍스트 임베딩 완료: {} 문자 -> {} 차원 벡터", text.length(), VECTOR_DIMENSION);
        return vector;
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