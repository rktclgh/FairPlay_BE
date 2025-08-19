package com.fairing.fairplay.ai.rag.controller;

import com.fairing.fairplay.ai.rag.service.VectorSearchService;
import com.fairing.fairplay.ai.rag.service.EventRagDataLoader;
import com.fairing.fairplay.ai.rag.service.ComprehensiveRagDataLoader;
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
    private final EventRagDataLoader eventRagDataLoader;
    private final ComprehensiveRagDataLoader comprehensiveRagDataLoader;

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

    /**
     * 이벤트 데이터만 RAG에 로드 (기존 기능)
     */
    @PostMapping("/load/events")
    public ResponseEntity<Map<String, Object>> loadEvents() {
        try {
            EventRagDataLoader.LoadResult result = eventRagDataLoader.loadAllEvents();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "이벤트 데이터 로드 완료",
                "totalCount", result.getTotalCount(),
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount(),
                "allSuccess", result.isAllSuccess()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "이벤트 데이터 로드 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 모든 공개 데이터를 RAG에 로드 (민감정보/통계/운영자 영역 제외)
     * 포함: Event, Booth, BoothExperience, Review, Category
     * 제외: User 개인정보, 통계 데이터, Admin 전용 데이터, 결제 정보
     */
    @PostMapping("/load/comprehensive")
    public ResponseEntity<Map<String, Object>> loadComprehensiveData() {
        try {
            ComprehensiveRagDataLoader.ComprehensiveLoadResult result = 
                comprehensiveRagDataLoader.loadAllPublicData();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "종합 공개 데이터 로드 완료",
                "totalSuccessCount", result.getTotalSuccessCount(),
                "totalFailCount", result.getTotalFailCount(),
                "summary", result.getSummary(),
                "details", Map.of(
                    "event", Map.of(
                        "total", result.eventResult.getTotalCount(),
                        "success", result.eventResult.getSuccessCount(),
                        "fail", result.eventResult.getFailCount()
                    ),
                    "booth", Map.of(
                        "total", result.boothResult.getTotalCount(),
                        "success", result.boothResult.getSuccessCount(),
                        "fail", result.boothResult.getFailCount()
                    ),
                    "boothExperience", Map.of(
                        "total", result.boothExperienceResult.getTotalCount(),
                        "success", result.boothExperienceResult.getSuccessCount(),
                        "fail", result.boothExperienceResult.getFailCount()
                    ),
                    "review", Map.of(
                        "total", result.reviewResult.getTotalCount(),
                        "success", result.reviewResult.getSuccessCount(),
                        "fail", result.reviewResult.getFailCount()
                    ),
                    "category", Map.of(
                        "total", result.categoryResult.getTotalCount(),
                        "success", result.categoryResult.getSuccessCount(),
                        "fail", result.categoryResult.getFailCount()
                    )
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "종합 데이터 로드 실패: " + e.getMessage()
            ));
        }
    }
}