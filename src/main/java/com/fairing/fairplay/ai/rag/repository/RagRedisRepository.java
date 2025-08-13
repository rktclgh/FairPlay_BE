package com.fairing.fairplay.ai.rag.repository;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

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

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CHUNKS_SET_KEY = "rag:chunks";
    private static final String CHUNK_HASH_PREFIX = "rag:chunk:";
    private static final String DOC_CHUNKS_SET_PREFIX = "rag:doc:";
    
    public void saveChunk(Chunk chunk) {
        String chunkKey = CHUNK_HASH_PREFIX + chunk.getChunkId();
        String docChunksKey = DOC_CHUNKS_SET_PREFIX + chunk.getDocId() + ":chunks";
        
        Map<String, Object> chunkData = Map.of(
            "docId", chunk.getDocId(),
            "text", chunk.getText(),
            "vector", encodeVector(chunk.getEmbedding()),
            "createdAt", chunk.getCreatedAt()
        );
        
        // 청크 저장
        redisTemplate.opsForHash().putAll(chunkKey, chunkData);
        
        // 청크 목록에 추가
        redisTemplate.opsForSet().add(CHUNKS_SET_KEY, chunk.getChunkId());
        
        // 문서-청크 역참조 추가
        redisTemplate.opsForSet().add(docChunksKey, chunk.getChunkId());
    }
    
    public Optional<Chunk> findChunk(String chunkId) {
        String chunkKey = CHUNK_HASH_PREFIX + chunkId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(chunkKey);
        
        if (data.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(Chunk.builder()
            .chunkId(chunkId)
            .docId((String) data.get("docId"))
            .text((String) data.get("text"))
            .embedding(decodeVector((String) data.get("vector")))
            .createdAt(Long.parseLong(data.get("createdAt").toString()))
            .build());
    }
    
    public List<Chunk> findAllChunks() {
        Set<Object> chunkIds = redisTemplate.opsForSet().members(CHUNKS_SET_KEY);
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Chunk> chunks = new ArrayList<>();
        for (Object chunkId : chunkIds) {
            findChunk(chunkId.toString()).ifPresent(chunks::add);
        }
        return chunks;
    }
    
    public void deleteDocument(String docId) {
        String docChunksKey = DOC_CHUNKS_SET_PREFIX + docId + ":chunks";
        Set<Object> chunkIds = redisTemplate.opsForSet().members(docChunksKey);
        
        if (chunkIds != null && !chunkIds.isEmpty()) {
            for (Object chunkId : chunkIds) {
                String chunkKey = CHUNK_HASH_PREFIX + chunkId.toString();
                redisTemplate.delete(chunkKey);
                redisTemplate.opsForSet().remove(CHUNKS_SET_KEY, chunkId);
            }
        }
        
        // 문서-청크 역참조 삭제
        redisTemplate.delete(docChunksKey);
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