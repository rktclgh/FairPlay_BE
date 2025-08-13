package com.fairing.fairplay.ai.rag.controller;

import com.fairing.fairplay.ai.rag.service.DocumentIngestService;
import com.fairing.fairplay.ai.rag.service.VectorSearchService;
import com.fairing.fairplay.ai.rag.service.EventRagDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG 관리 API 컨트롤러
 * 인제스트, 삭제, 상태 확인 등
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final DocumentIngestService documentIngestService;
    private final VectorSearchService vectorSearchService;
    private final EventRagDataLoader eventRagDataLoader;

    /**
     * 텍스트 문서 인제스트
     */
    @PostMapping("/ingest")
    public ResponseEntity<?> ingestText(@RequestBody IngestRequest request) {
        try {
            if (request.getTitle() == null || request.getContent() == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "title과 content는 필수입니다.")
                );
            }
            
            DocumentIngestService.IngestResult result = documentIngestService.ingestText(
                request.getTitle(),
                request.getContent(),
                request.getCategory()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", result.isSuccess(),
                "docId", result.getDocId(),
                "totalChunks", result.getTotalChunks(),
                "processedChunks", result.getProcessedChunks(),
                "failedChunks", result.getFailedChunks(),
                "errorMessage", result.getErrorMessage()
            ));
            
        } catch (Exception e) {
            log.error("인제스트 API 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "인제스트 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 문서 삭제
     */
    @DeleteMapping("/document/{docId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String docId) {
        try {
            documentIngestService.deleteDocument(docId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "문서가 삭제되었습니다.",
                "docId", docId
            ));
        } catch (Exception e) {
            log.error("문서 삭제 오류: {}", docId, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "문서 삭제 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 전체 문서 삭제 (개발/테스트용)
     */
    @DeleteMapping("/documents/all")
    public ResponseEntity<?> clearAllDocuments() {
        try {
            documentIngestService.clearAllDocuments();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "모든 문서가 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("전체 문서 삭제 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "전체 문서 삭제 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 캐시 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            Map<String, Object> cacheStatus = vectorSearchService.getCacheStatus();
            return ResponseEntity.ok(Map.of(
                "cache", cacheStatus,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("상태 확인 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "상태 확인 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 캐시 재로드
     */
    @PostMapping("/cache/reload")
    public ResponseEntity<?> reloadCache() {
        try {
            vectorSearchService.initializeCache();
            Map<String, Object> cacheStatus = vectorSearchService.getCacheStatus();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캐시가 재로드되었습니다.",
                "cache", cacheStatus
            ));
        } catch (Exception e) {
            log.error("캐시 재로드 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "캐시 재로드 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 이벤트 데이터 RAG 로드
     */
    @PostMapping("/load/events")
    public ResponseEntity<?> loadEvents() {
        try {
            EventRagDataLoader.LoadResult result = eventRagDataLoader.loadAllEvents();
            
            return ResponseEntity.ok(Map.of(
                "success", result.isAllSuccess(),
                "totalCount", result.getTotalCount(),
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount(),
                "message", "이벤트 데이터 로드가 완료되었습니다."
            ));
        } catch (Exception e) {
            log.error("이벤트 데이터 로드 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "이벤트 데이터 로드 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    /**
     * 특정 이벤트 RAG 로드
     */
    @PostMapping("/load/event/{eventId}")
    public ResponseEntity<?> loadSingleEvent(@PathVariable Long eventId) {
        try {
            boolean success = eventRagDataLoader.loadSingleEvent(eventId);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "eventId", eventId,
                "message", success ? "이벤트 로드 성공" : "이벤트 로드 실패"
            ));
        } catch (Exception e) {
            log.error("이벤트 로드 오류: {}", eventId, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "이벤트 로드 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 검색 테스트 (개발용)
     */
    @GetMapping("/search/test")
    public ResponseEntity<?> testSearch(@RequestParam String query, 
                                       @RequestParam(defaultValue = "5") int topK) {
        try {
            var result = vectorSearchService.search(query, topK);
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "topK", topK,
                "totalChunks", result.getTotalChunks(),
                "foundChunks", result.getChunks().size(),
                "contextLength", result.getContextText().length(),
                "chunks", result.getChunks().stream()
                    .map(chunk -> Map.of(
                        "chunkId", chunk.getChunk().getChunkId(),
                        "similarity", Math.round(chunk.getSimilarity() * 1000.0) / 1000.0,
                        "text", chunk.getChunk().getText().length() > 200 ? 
                            chunk.getChunk().getText().substring(0, 200) + "..." : 
                            chunk.getChunk().getText()
                    ))
                    .toList()
            ));
        } catch (Exception e) {
            log.error("검색 테스트 오류", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "검색 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    
    /**
     * 인제스트 요청 DTO
     */
    public static class IngestRequest {
        private String title;
        private String content;
        private String category;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}