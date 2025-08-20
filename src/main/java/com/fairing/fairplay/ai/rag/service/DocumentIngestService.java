package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.Document;
import com.fairing.fairplay.ai.rag.repository.RagRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * 문서 인제스트 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final RagRedisRepository repository;
    private final VectorSearchService vectorSearchService;
    private final ThreadPoolTaskExecutor taskExecutor;

    /**
     * 문서를 청킹하고 임베딩하여 저장
     */
    @Transactional
    public IngestResult ingestDocument(Document document) {
        log.info("문서 인제스트 시작: {} ({})", document.getTitle(), document.getDocId());
        
        try {
            // 기존 문서가 있다면 삭제
            repository.deleteDocument(document.getDocId());
            
            // 청킹
            List<Chunk> chunks = chunkingService.chunkDocument(
                document.getDocId(), 
                document.getContent()
            );
            
            log.info("청킹 완료: {} 개 청크 생성", chunks.size());
            
            // 비동기 임베딩 생성 및 저장 (병렬 처리로 성능 대폭 개선)
            List<CompletableFuture<ChunkProcessResult>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> processChunk(chunk), taskExecutor))
                .collect(Collectors.toList());
            
            // 모든 청크 처리 완료까지 대기
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            try {
                allOf.join(); // 모든 작업 완료 대기
                
                // 결과 집계
                int processedChunks = 0;
                int failedChunks = 0;
                List<Chunk> processedChunkList = new ArrayList<>();
                
                for (CompletableFuture<ChunkProcessResult> future : futures) {
                    ChunkProcessResult result = future.get();
                    if (result.isSuccess()) {
                        processedChunks++;
                        processedChunkList.add(result.getChunk());
                    } else {
                        failedChunks++;
                        log.error("청크 처리 실패: {} - {}", 
                            result.getChunk().getChunkId(), result.getErrorMessage());
                    }
                }
                
                // 성공한 청크들을 배치로 Redis에 저장
                if (!processedChunkList.isEmpty()) {
                    repository.saveChunks(processedChunkList);
                }
                
                log.info("병렬 청크 처리 완료: 성공 {}, 실패 {}", processedChunks, failedChunks);
                
            } catch (Exception e) {
                log.error("병렬 청크 처리 중 오류 발생", e);
                throw new RuntimeException("문서 인제스트 실패: " + e.getMessage(), e);
            }
            
            // 처리 결과 계산
            int processedChunks = (int) futures.stream()
                .mapToInt(future -> {
                    try {
                        return future.get().isSuccess() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                }).sum();
            
            int failedChunks = chunks.size() - processedChunks;
            
            // 벡터 검색 캐시 무효화 (새 데이터 반영)
            vectorSearchService.invalidateCache();
            
            IngestResult result = IngestResult.builder()
                .docId(document.getDocId())
                .totalChunks(chunks.size())
                .processedChunks(processedChunks)
                .failedChunks(failedChunks)
                .success(failedChunks == 0)
                .build();
            
            log.info("문서 인제스트 완료: {} - 처리: {}, 실패: {}", 
                document.getDocId(), processedChunks, failedChunks);
                
            return result;
            
        } catch (Exception e) {
            log.error("문서 인제스트 실패: {} - {}", document.getDocId(), e.getMessage(), e);
            
            return IngestResult.builder()
                .docId(document.getDocId())
                .totalChunks(0)
                .processedChunks(0)
                .failedChunks(0)
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    /**
     * 텍스트로 직접 문서 인제스트
     */
    public IngestResult ingestText(String title, String content, String category) {
        String docId = generateDocId();
        
        Document document = Document.builder()
            .docId(docId)
            .title(title)
            .content(content)
            .category(category != null ? category : "general")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
        
        return ingestDocument(document);
    }
    
    /**
     * 문서 삭제
     */
    public void deleteDocument(String docId) {
        log.info("문서 삭제: {}", docId);
        repository.deleteDocument(docId);
        
        // 캐시 무효화
        vectorSearchService.invalidateCache();
        
        log.info("문서 삭제 완료: {}", docId);
    }
    
    /**
     * 전체 문서 삭제 (테스트용)
     */
    public void clearAllDocuments() {
        log.warn("전체 문서 삭제 시작");
        
        // Redis의 모든 RAG 데이터 삭제
        repository.clearAllData();
        
        // 캐시 무효화
        vectorSearchService.invalidateCache();
        
        log.warn("전체 문서 삭제 완료");
    }
    
    /**
     * 개별 청크를 비동기로 처리 (임베딩 생성)
     */
    private ChunkProcessResult processChunk(Chunk chunk) {
        try {
            log.debug("청크 처리 시작: {}, docId: {}, text length: {}", 
                chunk.getChunkId(), chunk.getDocId(), chunk.getText().length());
            
            // 임베딩 생성
            float[] embedding = embeddingService.embedText(chunk.getText());
            chunk.setEmbedding(embedding);
            
            log.debug("임베딩 생성 완료: {} ({} 차원)", chunk.getChunkId(), embedding.length);
            
            return new ChunkProcessResult(chunk, true, null);
            
        } catch (Exception e) {
            log.error("청크 임베딩 처리 실패: {} - {}", chunk.getChunkId(), e.getMessage(), e);
            return new ChunkProcessResult(chunk, false, e.getMessage());
        }
    }
    
    private String generateDocId() {
        return "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * 청크 처리 결과 (비동기 처리용)
     */
    private static class ChunkProcessResult {
        private final Chunk chunk;
        private final boolean success;
        private final String errorMessage;
        
        public ChunkProcessResult(Chunk chunk, boolean success, String errorMessage) {
            this.chunk = chunk;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public Chunk getChunk() { return chunk; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 인제스트 결과
     */
    public static class IngestResult {
        private final String docId;
        private final int totalChunks;
        private final int processedChunks;
        private final int failedChunks;
        private final boolean success;
        private final String errorMessage;
        
        public static IngestResultBuilder builder() {
            return new IngestResultBuilder();
        }
        
        private IngestResult(String docId, int totalChunks, int processedChunks, 
                           int failedChunks, boolean success, String errorMessage) {
            this.docId = docId;
            this.totalChunks = totalChunks;
            this.processedChunks = processedChunks;
            this.failedChunks = failedChunks;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public String getDocId() { return docId; }
        public int getTotalChunks() { return totalChunks; }
        public int getProcessedChunks() { return processedChunks; }
        public int getFailedChunks() { return failedChunks; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        public static class IngestResultBuilder {
            private String docId;
            private int totalChunks;
            private int processedChunks;
            private int failedChunks;
            private boolean success;
            private String errorMessage;
            
            public IngestResultBuilder docId(String docId) { this.docId = docId; return this; }
            public IngestResultBuilder totalChunks(int totalChunks) { this.totalChunks = totalChunks; return this; }
            public IngestResultBuilder processedChunks(int processedChunks) { this.processedChunks = processedChunks; return this; }
            public IngestResultBuilder failedChunks(int failedChunks) { this.failedChunks = failedChunks; return this; }
            public IngestResultBuilder success(boolean success) { this.success = success; return this; }
            public IngestResultBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
            
            public IngestResult build() {
                return new IngestResult(docId, totalChunks, processedChunks, failedChunks, success, errorMessage);
            }
        }
    }
}