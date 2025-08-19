package com.fairing.fairplay.ai.rag.controller;

import com.fairing.fairplay.ai.rag.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG 시스템 관리용 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
public class RagAdminController {

    private final VectorSearchService vectorSearchService;

    /**
     * 캐시 상태 확인
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(vectorSearchService.getCacheStatus());
    }

    /**
     * 캐시 무효화
     */
    @PostMapping("/cache/invalidate")
    public ResponseEntity<String> invalidateCache() {
        vectorSearchService.invalidateCache();
        return ResponseEntity.ok("캐시가 무효화되었습니다.");
    }

    /**
     * 모든 임베딩 데이터 삭제 (차원 변경 시 사용)
     * WARNING: 이 작업은 되돌릴 수 없습니다!
     */
    @DeleteMapping("/embeddings/all")
    public ResponseEntity<String> clearAllEmbeddingData() {
        vectorSearchService.clearAllEmbeddingData();
        return ResponseEntity.ok("모든 임베딩 데이터가 삭제되었습니다. 문서를 다시 업로드하여 새로운 임베딩을 생성해주세요.");
    }

    /**
     * 캐시 강제 초기화
     */
    @PostMapping("/cache/initialize")
    public ResponseEntity<String> initializeCache() {
        try {
            vectorSearchService.initializeCache();
            return ResponseEntity.ok("캐시가 초기화되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("캐시 초기화 실패: " + e.getMessage());
        }
    }
}