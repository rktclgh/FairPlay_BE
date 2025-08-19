package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Document;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothExperience;
import com.fairing.fairplay.booth.repository.BoothExperienceRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.MainCategory;
import com.fairing.fairplay.event.entity.SubCategory;
import com.fairing.fairplay.event.repository.EventDetailRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.MainCategoryRepository;
import com.fairing.fairplay.event.repository.SubCategoryRepository;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 민감정보/통계/운영자 영역을 제외한 모든 공개 데이터를 RAG에 로드하는 종합 로더
 * 포함 영역: Event/EventDetail, Booth/BoothExperience, Review, Category
 * 제외 영역: User 개인정보, 통계 데이터, Admin 전용 데이터, 결제 정보
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveRagDataLoader {

    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BoothRepository boothRepository;
    private final BoothExperienceRepository boothExperienceRepository;
    private final ReviewRepository reviewRepository;
    private final DocumentIngestService documentIngestService;

    /**
     * 모든 공개 데이터를 RAG에 로드
     */
    @Transactional(readOnly = true)
    public ComprehensiveLoadResult loadAllPublicData() {
        log.info("종합 공개 데이터 RAG 로드 시작...");
        
        ComprehensiveLoadResult result = new ComprehensiveLoadResult();
        
        // 1. 이벤트 데이터 로드
        result.eventResult = loadEvents();
        
        // 2. 부스 데이터 로드
        result.boothResult = loadBooths();
        
        // 3. 부스 체험 데이터 로드
        result.boothExperienceResult = loadBoothExperiences();
        
        // 4. 리뷰 데이터 로드 (개인정보 제외)
        result.reviewResult = loadReviews();
        
        // 5. 카테고리 데이터 로드
        result.categoryResult = loadCategories();
        
        log.info("종합 공개 데이터 RAG 로드 완료: {}", result.getSummary());
        
        return result;
    }
    
    /**
     * 이벤트 데이터 로드
     */
    private LoadResult loadEvents() {
        log.info("이벤트 데이터 로드 중...");
        List<Event> events = eventRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (Event event : events) {
            try {
                EventDetail eventDetail = eventDetailRepository.findById(event.getEventId()).orElse(null);
                Document document = buildEventDocument(event, eventDetail);
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("이벤트 로드 실패: {} - {}", event.getEventId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("이벤트 로드 오류: {} - {}", event.getEventId(), e.getMessage());
            }
        }
        
        return new LoadResult("Event", events.size(), successCount, failCount);
    }
    
    /**
     * 부스 데이터 로드
     */
    private LoadResult loadBooths() {
        log.info("부스 데이터 로드 중...");
        List<Booth> booths = boothRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (Booth booth : booths) {
            try {
                Document document = buildBoothDocument(booth);
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("부스 로드 실패: {} - {}", booth.getId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("부스 로드 오류: {} - {}", booth.getId(), e.getMessage());
            }
        }
        
        return new LoadResult("Booth", booths.size(), successCount, failCount);
    }
    
    /**
     * 부스 체험 데이터 로드
     */
    private LoadResult loadBoothExperiences() {
        log.info("부스 체험 데이터 로드 중...");
        List<BoothExperience> experiences = boothExperienceRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (BoothExperience experience : experiences) {
            try {
                Document document = buildBoothExperienceDocument(experience);
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("부스 체험 로드 실패: {} - {}", experience.getExperienceId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("부스 체험 로드 오류: {} - {}", experience.getExperienceId(), e.getMessage());
            }
        }
        
        return new LoadResult("BoothExperience", experiences.size(), successCount, failCount);
    }
    
    /**
     * 리뷰 데이터 로드 (개인정보 제외)
     */
    private LoadResult loadReviews() {
        log.info("리뷰 데이터 로드 중...");
        List<Review> reviews = reviewRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (Review review : reviews) {
            try {
                Document document = buildReviewDocument(review);
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("리뷰 로드 실패: {} - {}", review.getId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("리뷰 로드 오류: {} - {}", review.getId(), e.getMessage());
            }
        }
        
        return new LoadResult("Review", reviews.size(), successCount, failCount);
    }
    
    /**
     * 카테고리 데이터 로드
     */
    private LoadResult loadCategories() {
        log.info("카테고리 데이터 로드 중...");
        List<MainCategory> categories = mainCategoryRepository.findAll();
        int successCount = 0;
        int failCount = 0;

        for (MainCategory category : categories) {
            try {
                List<SubCategory> subCategories = subCategoryRepository.findByMainCategory(category);
                Document document = buildCategoryDocument(category, subCategories);

                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("카테고리 로드 실패: {} - {}", category.getGroupId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("카테고리 로드 오류: {} - {}", category.getGroupId(), e.getMessage());
            }
        }

        return new LoadResult("Category", categories.size(), successCount, failCount);
    }
    
    /**
     * Event + EventDetail을 RAG Document로 변환
     */
    private Document buildEventDocument(Event event, EventDetail eventDetail) {
        StringBuilder content = new StringBuilder();
        
        content.append("=== 이벤트 정보 ===\n");
        content.append("이벤트 ID: ").append(event.getEventId()).append("\n");
        
        if (event.getTitleKr() != null) {
            content.append("제목(한국어): ").append(event.getTitleKr()).append("\n");
        }
        if (event.getTitleEng() != null) {
            content.append("제목(영어): ").append(event.getTitleEng()).append("\n");
        }

        if (eventDetail.getSubCategory() != null && eventDetail.getSubCategory().getCategoryName() != null) {
            content.append("카테고리: ").append(eventDetail.getSubCategory().getCategoryName()).append("\n");
        }
        
        if (eventDetail != null) {
            if (eventDetail.getContent() != null) {
                content.append("\n=== 이벤트 내용 ===\n");
                content.append(removeHtmlTags(eventDetail.getContent())).append("\n");
            }
            if (eventDetail.getPolicy() != null) {
                content.append("\n=== 이벤트 정책 ===\n");
                content.append(removeHtmlTags(eventDetail.getPolicy())).append("\n");
            }
            if (eventDetail.getStartDate() != null) {
                content.append("시작일: ").append(eventDetail.getStartDate()).append("\n");
            }
            if (eventDetail.getEndDate() != null) {
                content.append("종료일: ").append(eventDetail.getEndDate()).append("\n");
            }
            if (eventDetail.getHostName() != null) {
                content.append("주최자: ").append(eventDetail.getHostName()).append("\n");
            }
            if (eventDetail.getContactInfo() != null) {
                content.append("연락처: ").append(eventDetail.getContactInfo()).append("\n");
            }
            if (eventDetail.getBio() != null) {
                content.append("소개: ").append(eventDetail.getBio()).append("\n");
            }
            if (eventDetail.getLocationDetail() != null) {
                content.append("장소 상세: ").append(eventDetail.getLocationDetail()).append("\n");
            }

            
            // 위치 정보
            if (eventDetail.getLocation() != null) {
                content.append("\n=== 위치 정보 ===\n");
                if (eventDetail.getLocation().getAddress() != null) {
                    content.append("주소: ").append(eventDetail.getLocation().getAddress()).append("\n");
                }
                if (eventDetail.getLocation().getPlaceName() != null) {
                    content.append("장소명: ").append(eventDetail.getLocation().getPlaceName()).append("\n");
                }
            }
        }
        
        return Document.builder()
            .docId("event_" + event.getEventId())
            .title(event.getTitleKr() != null ? event.getTitleKr() : "이벤트 " + event.getEventId())
            .content(content.toString())
            .category("event")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Booth를 RAG Document로 변환
     */
    private Document buildBoothDocument(Booth booth) {
        StringBuilder content = new StringBuilder();
        
        content.append("=== 부스 정보 ===\n");
        content.append("부스 ID: ").append(booth.getId()).append("\n");
        
        if (booth.getBoothTitle() != null) {
            content.append("부스명: ").append(booth.getBoothTitle()).append("\n");
        }
        if (booth.getBoothDescription() != null) {
            content.append("부스 설명: ").append(booth.getBoothDescription()).append("\n");
        }
        if (booth.getLocation() != null) {
            content.append("부스 위치: ").append(booth.getLocation()).append("\n");
        }
        if (booth.getStartDate() != null) {
            content.append("시작일: ").append(booth.getStartDate()).append("\n");
        }
        if (booth.getEndDate() != null) {
            content.append("종료일: ").append(booth.getEndDate()).append("\n");
        }
        
        // 이벤트 정보 (부스가 속한 이벤트)
        if (booth.getEvent() != null) {
            content.append("\n=== 소속 이벤트 ===\n");
            if (booth.getEvent().getTitleKr() != null) {
                content.append("이벤트명: ").append(booth.getEvent().getTitleKr()).append("\n");
            }
        }
        
        return Document.builder()
            .docId("booth_" + booth.getId())
            .title(booth.getBoothTitle() != null ? booth.getBoothTitle() : "부스 " + booth.getId())
            .content(content.toString())
            .category("booth")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * BoothExperience를 RAG Document로 변환
     */
    private Document buildBoothExperienceDocument(BoothExperience experience) {
        StringBuilder content = new StringBuilder();
        
        content.append("=== 부스 체험 정보 ===\n");
        content.append("체험 ID: ").append(experience.getExperienceId()).append("\n");
        
        if (experience.getTitle() != null) {
            content.append("체험명: ").append(experience.getTitle()).append("\n");
        }
        if (experience.getDescription() != null) {
            content.append("체험 설명: ").append(experience.getDescription()).append("\n");
        }
        if (experience.getDurationMinutes() != null) {
            content.append("소요 시간: ").append(experience.getDurationMinutes()).append("분\n");
        }
        if (experience.getMaxCapacity() != null) {
            content.append("최대 참가자 수: ").append(experience.getMaxCapacity()).append("명\n");
        }
        
        // 부스 정보
        if (experience.getBooth() != null) {
            content.append("\n=== 소속 부스 ===\n");
            if (experience.getBooth().getBoothTitle() != null) {
                content.append("부스명: ").append(experience.getBooth().getBoothTitle()).append("\n");
            }
        }
        
        return Document.builder()
            .docId("booth_experience_" + experience.getExperienceId())
            .title(experience.getTitle() != null ?
                experience.getTitle() : "부스 체험 " + experience.getExperienceId())
            .content(content.toString())
            .category("booth_experience")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Review를 RAG Document로 변환 (개인정보 제외)
     */
    private Document buildReviewDocument(Review review) {
        StringBuilder content = new StringBuilder();
        
        content.append("=== 리뷰 정보 ===\n");
        content.append("리뷰 ID: ").append(review.getId()).append("\n");
        
        if (review.getComment() != null) {
            content.append("리뷰 내용: ").append(review.getComment()).append("\n");
        }
        if (review.getStar() != null) {
            content.append("평점: ").append(review.getStar()).append("점\n");
        }
        if (review.getCreatedAt() != null) {
            content.append("작성일: ").append(review.getCreatedAt()).append("\n");
        }
        
        // 이벤트 정보 (리뷰 대상 이벤트)
        if (review.getReservation() != null) {
            content.append("\n=== 리뷰 대상 이벤트 ===\n");
            if (review.getReservation().getEvent().getTitleKr() != null) {
                content.append("이벤트명: ").append(review.getReservation().getEvent().getTitleKr()).append("\n");
            }
        }
        
        // 개인정보(사용자 정보)는 제외하고 익명화된 정보만 포함
        content.append("작성자: 익명 사용자\n");
        
        return Document.builder()
            .docId("review_" + review.getId())
            .title("리뷰 " + review.getId())
            .content(content.toString())
            .category("review")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * MainCategory를 RAG Document로 변환
     */
    private Document buildCategoryDocument(MainCategory category, List<SubCategory> subCategories) {
        StringBuilder content = new StringBuilder();

        content.append("=== 카테고리 정보 ===\n");
        content.append("카테고리 ID: ").append(category.getGroupId()).append("\n");

        if (category.getGroupName() != null) {
            content.append("카테고리명: ").append(category.getGroupName()).append("\n");
        }
        // if (category.getDescription() != null) { ... }  // 필요 시 복원

        // 하위 카테고리 정보
        if (subCategories != null && !subCategories.isEmpty()) {
            content.append("\n=== 하위 카테고리 ===\n");
            for (SubCategory sc : subCategories) {
                if (sc != null && sc.getCategoryName() != null) {
                    content.append("- ").append(sc.getCategoryName()).append("\n");
                }
            }
        }
        
        return Document.builder()
            .docId("category_" + category.getGroupId())
            .title(category.getGroupName() != null ? category.getGroupName() : "카테고리 " + category.getGroupId())
            .content(content.toString())
            .category("category")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * HTML 태그 제거
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        String cleanText = html.replaceAll("<[^>]*>", "");
        cleanText = cleanText
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ");
        
        return cleanText.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * 개별 도메인 로드 결과
     */
    public static class LoadResult {
        private final String domain;
        private final int totalCount;
        private final int successCount;
        private final int failCount;
        
        public LoadResult(String domain, int totalCount, int successCount, int failCount) {
            this.domain = domain;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failCount = failCount;
        }
        
        public String getDomain() { return domain; }
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public boolean isAllSuccess() { return failCount == 0; }
        
        @Override
        public String toString() {
            return String.format("%s: %d개 중 %d개 성공, %d개 실패", 
                domain, totalCount, successCount, failCount);
        }
    }
    
    /**
     * 종합 로드 결과
     */
    public static class ComprehensiveLoadResult {
        public LoadResult eventResult;
        public LoadResult boothResult;
        public LoadResult boothExperienceResult;
        public LoadResult reviewResult;
        public LoadResult categoryResult;
        
        public String getSummary() {
            return String.format("Event: %s, Booth: %s, BoothExp: %s, Review: %s, Category: %s",
                eventResult, boothResult, boothExperienceResult, reviewResult, categoryResult);
        }
        
        public int getTotalSuccessCount() {
            return eventResult.getSuccessCount() + 
                   boothResult.getSuccessCount() + 
                   boothExperienceResult.getSuccessCount() + 
                   reviewResult.getSuccessCount() + 
                   categoryResult.getSuccessCount();
        }
        
        public int getTotalFailCount() {
            return eventResult.getFailCount() + 
                   boothResult.getFailCount() + 
                   boothExperienceResult.getFailCount() + 
                   reviewResult.getFailCount() + 
                   categoryResult.getFailCount();
        }
    }
}