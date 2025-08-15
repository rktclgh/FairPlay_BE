package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Document;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.EventDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Event/EventDetail 데이터를 RAG용 문서로 변환하는 로더
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventRagDataLoader {

    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final DocumentIngestService documentIngestService;

    /**
     * 모든 이벤트 데이터를 RAG에 로드
     */
    @Transactional(readOnly = true)
    public LoadResult loadAllEvents() {
        log.info("이벤트 데이터 RAG 로드 시작...");
        
        List<Event> events = eventRepository.findAll();
        int totalEvents = events.size();
        int successCount = 0;
        int failCount = 0;
        
        for (Event event : events) {
            try {
                // EventDetail 조회
                EventDetail eventDetail = eventDetailRepository.findById(event.getEventId()).orElse(null);
                
                // Document 생성
                Document document = buildDocumentFromEvent(event, eventDetail);
                
                // RAG에 인제스트
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                
                if (result.isSuccess()) {
                    successCount++;
                    log.debug("이벤트 RAG 로드 성공: {} (청크 {}개)", event.getEventId(), result.getProcessedChunks());
                } else {
                    failCount++;
                    log.warn("이벤트 RAG 로드 실패: {} - {}", event.getEventId(), result.getErrorMessage());
                }
                
            } catch (Exception e) {
                failCount++;
                log.error("이벤트 RAG 로드 오류: {} - {}", event.getEventId(), e.getMessage(), e);
            }
        }
        
        log.info("이벤트 데이터 RAG 로드 완료: 총 {}, 성공 {}, 실패 {}", totalEvents, successCount, failCount);
        
        return new LoadResult(totalEvents, successCount, failCount);
    }
    
    /**
     * 특정 이벤트만 RAG에 로드
     */
    @Transactional(readOnly = true)
    public boolean loadSingleEvent(Long eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.warn("이벤트를 찾을 수 없음: {}", eventId);
                return false;
            }
            
            EventDetail eventDetail = eventDetailRepository.findById(eventId).orElse(null);
            Document document = buildDocumentFromEvent(event, eventDetail);
            
            DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
            
            if (result.isSuccess()) {
                log.info("이벤트 RAG 로드 성공: {} (청크 {}개)", eventId, result.getProcessedChunks());
                return true;
            } else {
                log.error("이벤트 RAG 로드 실패: {} - {}", eventId, result.getErrorMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("이벤트 RAG 로드 오류: {} - {}", eventId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Event + EventDetail을 RAG Document로 변환
     */
    private Document buildDocumentFromEvent(Event event, EventDetail eventDetail) {
        StringBuilder content = new StringBuilder();
        
        // 기본 이벤트 정보
        content.append("=== 이벤트 정보 ===\n");
        content.append("이벤트 ID: ").append(event.getEventId()).append("\n");
        
        // Event 기본 정보
        if (event.getTitleKr() != null) {
            content.append("제목(한국어): ").append(event.getTitleKr()).append("\n");
        }
        if (event.getTitleEng() != null) {
            content.append("제목(영어): ").append(event.getTitleEng()).append("\n");
        }
        content.append("이벤트 코드: ").append(event.getEventCode()).append("\n");
        
        // EventDetail이 있는 경우 상세 정보 추가
        if (eventDetail != null) {
            if (eventDetail.getContent() != null) {
                content.append("\n=== 이벤트 내용 ===\n");
                // HTML 태그 제거 처리
                String cleanContent = removeHtmlTags(eventDetail.getContent());
                content.append(cleanContent).append("\n");
            }
            if (eventDetail.getPolicy() != null) {
                content.append("\n=== 이벤트 정책 ===\n");
                // HTML 태그 제거 처리
                String cleanPolicy = removeHtmlTags(eventDetail.getPolicy());
                content.append(cleanPolicy).append("\n");
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
            
            // 위치 정보 추가 (Location 엔티티)
            if (eventDetail.getLocation() != null) {
                content.append("\n=== 위치 정보 ===\n");
                if (eventDetail.getLocation().getAddress() != null) {
                    content.append("주소: ").append(eventDetail.getLocation().getAddress()).append("\n");
                }
                if (eventDetail.getLocation().getPlaceName() != null) {
                    content.append("장소명: ").append(eventDetail.getLocation().getPlaceName()).append("\n");
                }
                if (eventDetail.getLocation().getLatitude() != null && eventDetail.getLocation().getLongitude() != null) {
                    content.append("위치 좌표: ").append(eventDetail.getLocation().getLatitude())
                           .append(", ").append(eventDetail.getLocation().getLongitude()).append("\n");
                }
            }
            if (eventDetail.getEventTime() != null) {
                content.append("소요 시간: ").append(eventDetail.getEventTime()).append("분\n");
            }
        }
        
        // Document 생성
        return Document.builder()
            .docId("event_" + event.getEventId())
            .title(event.getTitleKr() != null ? 
                event.getTitleKr() : "이벤트 " + event.getEventId())
            .content(content.toString())
            .category("event")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();
    }
    
    /**
     * HTML 태그 제거 (간단한 정규식 사용)
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        // HTML 태그 제거
        String cleanText = html.replaceAll("<[^>]*>", "");
        
        // HTML 엔티티 디코딩 (기본적인 것들만)
        cleanText = cleanText
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ");
        
        // 연속된 공백을 하나로 합치고 trim
        cleanText = cleanText.replaceAll("\\s+", " ").trim();
        
        return cleanText;
    }
    
    /**
     * 로드 결과
     */
    public static class LoadResult {
        private final int totalCount;
        private final int successCount;
        private final int failCount;
        
        public LoadResult(int totalCount, int successCount, int failCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failCount = failCount;
        }
        
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public boolean isAllSuccess() { return failCount == 0; }
    }
}