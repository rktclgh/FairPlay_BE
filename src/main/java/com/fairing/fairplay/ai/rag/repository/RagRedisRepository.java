package com.fairing.fairplay.ai.rag.repository;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.HashMap;

/**
 * RAG Redis 저장소
 * 키 접두사: rag:*
 * - rag:chunks (Set) : 등록된 chunkId 목록
 * - rag:chunk:{chunkId} (Hash) : docId, text, vector(Base64)
 * - rag:doc:{docId}:chunks (Set) : 문서→청크 역참조
 */
@Repository
@RequiredArgsConstructor
public class RagRedisRepository {

    private final StringRedisTemplate redisTemplate;
    
    private static final String CHUNKS_SET_KEY = "rag:chunks";
    private static final String CHUNK_HASH_PREFIX = "rag:chunk:";
    private static final String DOC_CHUNKS_SET_PREFIX = "rag:doc:";
    
    /**
     * 단일 청크 저장 (기존 메서드 유지)
     */
    public void saveChunk(Chunk chunk) {
        String chunkKey = CHUNK_HASH_PREFIX + chunk.getChunkId();
        String docChunksKey = DOC_CHUNKS_SET_PREFIX + chunk.getDocId() + ":chunks";
        
        try {
            // 개별적으로 필드 저장하여 타입 이슈 방지
            redisTemplate.opsForHash().put(chunkKey, "docId", chunk.getDocId());
            redisTemplate.opsForHash().put(chunkKey, "text", chunk.getText());
            float[] embedding = chunk.getEmbedding();
            if (embedding != null) {
                redisTemplate.opsForHash().put(chunkKey, "vector", encodeVector(embedding));
            }
            redisTemplate.opsForHash().put(chunkKey, "createdAt", chunk.getCreatedAt());
            
            // 청크 목록에 추가
            redisTemplate.opsForSet().add(CHUNKS_SET_KEY, chunk.getChunkId());
            
            // 문서-청크 역참조 추가
            redisTemplate.opsForSet().add(docChunksKey, chunk.getChunkId());
        } catch (Exception e) {
            throw new RuntimeException("청크 저장 실패: " + chunk.getChunkId() + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 여러 청크를 배치로 저장 (파이프라이닝 사용으로 성능 개선)
     */
    public void saveChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        
        try {
            // Redis 파이프라이닝을 사용하여 배치 저장 (RedisCallback 사용)
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (Chunk chunk : chunks) {
                    String chunkKey = CHUNK_HASH_PREFIX + chunk.getChunkId();
                    String docChunksKey = DOC_CHUNKS_SET_PREFIX + chunk.getDocId() + ":chunks";
                    
                    // 해시 필드 저장
                    redisTemplate.opsForHash().put(chunkKey, "docId", chunk.getDocId());
                    redisTemplate.opsForHash().put(chunkKey, "text", chunk.getText());
                    
                    float[] embedding = chunk.getEmbedding();
                    if (embedding != null) {
                        redisTemplate.opsForHash().put(chunkKey, "vector", encodeVector(embedding));
                    }
                    redisTemplate.opsForHash().put(chunkKey, "createdAt", chunk.getCreatedAt());
                    
                    // 세트에 추가
                    redisTemplate.opsForSet().add(CHUNKS_SET_KEY, chunk.getChunkId());
                    redisTemplate.opsForSet().add(docChunksKey, chunk.getChunkId());
                }
                return null;
            });
            
        } catch (Exception e) {
            throw new RuntimeException("배치 청크 저장 실패: " + e.getMessage(), e);
        }
    }
    
    public Optional<Chunk> findChunk(String chunkId) {
        String chunkKey = CHUNK_HASH_PREFIX + chunkId;
        Map<Object, Object> rawData = redisTemplate.opsForHash().entries(chunkKey);
        
        if (rawData.isEmpty()) {
            return Optional.empty();
        }
        
        // Object에서 String으로 안전하게 변환
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawData.entrySet()) {
            data.put(entry.getKey().toString(), entry.getValue().toString());
        }
        
        // createdAt 값은 String으로 그대로 사용
        String createdAt = data.get("createdAt");
        if (createdAt == null) {
            createdAt = String.valueOf(System.currentTimeMillis());
        }
        
        return Optional.of(Chunk.builder()
            .chunkId(chunkId)
            .docId(data.get("docId"))
            .text(data.get("text"))
            .embedding(data.get("vector") != null && !data.get("vector").isEmpty()
                ? decodeVector(data.get("vector"))
                : null)
            .createdAt(createdAt)
            .build());
    }
    
    public List<Chunk> findAllChunks() {
        Set<String> chunkIds = redisTemplate.opsForSet().members(CHUNKS_SET_KEY);
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Chunk> chunks = new ArrayList<>();
        for (String chunkId : chunkIds) {
            findChunk(chunkId).ifPresent(chunks::add);
        }
        return chunks;
    }
    
    public void deleteDocument(String docId) {
        String docChunksKey = DOC_CHUNKS_SET_PREFIX + docId + ":chunks";
        Set<String> chunkIds = redisTemplate.opsForSet().members(docChunksKey);
        
        if (chunkIds != null && !chunkIds.isEmpty()) {
            for (String chunkId : chunkIds) {
                String chunkKey = CHUNK_HASH_PREFIX + chunkId;
                redisTemplate.delete(chunkKey);
                redisTemplate.opsForSet().remove(CHUNKS_SET_KEY, chunkId);
            }
        }
        
        // 문서-청크 역참조 삭제
        redisTemplate.delete(docChunksKey);
    }
    
    /**
     * 모든 RAG 데이터 삭제 (타입 오류 해결용)
     */
    public void clearAllData() {
        try {
            // 모든 rag:* 키 삭제
            Set<String> keys = redisTemplate.keys("rag:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 에러 발생시 개별 키 삭제 시도
            try {
                redisTemplate.delete(CHUNKS_SET_KEY);
                Set<String> chunkKeys = redisTemplate.keys(CHUNK_HASH_PREFIX + "*");
                if (chunkKeys != null && !chunkKeys.isEmpty()) {
                    redisTemplate.delete(chunkKeys);
                }
                Set<String> docKeys = redisTemplate.keys(DOC_CHUNKS_SET_PREFIX + "*");
                if (docKeys != null && !docKeys.isEmpty()) {
                    redisTemplate.delete(docKeys);
                }
            } catch (Exception ex) {
                // 최후의 수단 - 로그만 남김
                System.err.println("RAG 데이터 삭제 실패: " + ex.getMessage());
            }
        }
    }
    
    public long getTotalChunkCount() {
        Long size = redisTemplate.opsForSet().size(CHUNKS_SET_KEY);
        return size != null ? size : 0;
    }
    
    /**
     * float[] 배열을 Base64로 인코딩 (little-endian)
     */
    private String encodeVector(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (float f : vector) {
            buffer.putFloat(f);
        }
        
        return Base64.getEncoder().encodeToString(buffer.array());
    }
    
    /**
     * Base64 문자열을 float[] 배열로 디코딩
     */
    private float[] decodeVector(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        
        return vector;
    }
}