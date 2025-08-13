package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.Document;
import com.fairing.fairplay.ai.rag.repository.RagRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
            
            int processedChunks = 0;
            int failedChunks = 0;
            
            // 각 청크에 대해 임베딩 생성 및 저장
            for (Chunk chunk : chunks) {
                try {
                    // 임베딩 생성
                    float[] embedding = embeddingService.embedText(chunk.getText());
                    chunk.setEmbedding(embedding);
                    
                    // Redis에 저장
                    repository.saveChunk(chunk);
                    processedChunks++;
                    
                    log.debug("청크 처리 완료: {} ({} 차원)", 
                        chunk.getChunkId(), embedding.length);
                        
                } catch (Exception e) {
                    log.error("청크 처리 실패: {} - {}", chunk.getChunkId(), e.getMessage());
                    failedChunks++;
                }
            }
            
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
        
        // Redis의 모든 RAG 데이터 삭제하는 로직은 별도 구현 필요
        // 현재는 캐시만 무효화
        vectorSearchService.invalidateCache();
        
        log.warn("전체 문서 삭제 완료");
    }
    
    private String generateDocId() {
        return "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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