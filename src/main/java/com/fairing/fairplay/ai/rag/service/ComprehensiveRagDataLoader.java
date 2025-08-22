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
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.PageRequest;

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
    
    // SubCategory 캐시 (N+1 문제 해결용)
    private Map<Integer, List<SubCategory>> subCategoryCache;
    private final BoothRepository boothRepository;
    private final BoothExperienceRepository boothExperienceRepository;
    private final ReviewRepository reviewRepository;
    private final TicketRepository ticketRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final AttendeeRepository attendeeRepository;
    private final DocumentIngestService documentIngestService;
    
    /**
     * 초기화 메서드 - SubCategory 캐시 생성 (N+1 문제 해결)
     */
    @PostConstruct
    public void init() {
        initializeSubCategoryCache();
    }
    
    /**
     * SubCategory 캐시 초기화
     */
    private void initializeSubCategoryCache() {
        try {
            List<SubCategory> allSubCategories = subCategoryRepository.findAll();
            subCategoryCache = allSubCategories.stream()
                .collect(Collectors.groupingBy(sub -> sub.getMainCategory().getGroupId()));
            
            log.info("SubCategory 캐시 초기화 완료: {} 개 MainCategory에 대한 SubCategory 매핑", 
                subCategoryCache.size());
        } catch (Exception e) {
            log.error("SubCategory 캐시 초기화 실패", e);
            subCategoryCache = new HashMap<>();
        }
    }

    /**
     * 데이터베이스 상태 확인 (디버깅용)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseStatus() {
        log.info("데이터베이스 상태 확인 중...");
        
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 이벤트 데이터 확인
            List<Event> events = eventRepository.findAll();
            Map<String, Object> eventStatus = new HashMap<>();
            eventStatus.put("count", events.size());
            
            if (!events.isEmpty()) {
                List<Map<String, Object>> eventSamples = new ArrayList<>();
                for (int i = 0; i < events.size(); i++) {
                    Event event = events.get(i);
                    Map<String, Object> sample = new HashMap<>();
                    sample.put("eventId", event.getEventId());
                    sample.put("titleKr", event.getTitleKr());
                    sample.put("titleEng", event.getTitleEng());
                    
                    EventDetail detail = eventDetailRepository.findById(event.getEventId()).orElse(null);
                    sample.put("hasDetail", detail != null);
                    if (detail != null) {
                        sample.put("hasContent", detail.getContent() != null && !detail.getContent().isBlank());
                        sample.put("contentLength", detail.getContent() != null ? detail.getContent().length() : 0);
                    }
                    
                    eventSamples.add(sample);
                }
                eventStatus.put("samples", eventSamples);
            }
            
            status.put("events", eventStatus);
            
            // 부스 데이터 확인
            List<Booth> booths = boothRepository.findAll();
            status.put("booths", Map.of("count", booths.size()));
            
            // 리뷰 데이터 확인
            List<Review> reviews = reviewRepository.findAll();
            status.put("reviews", Map.of("count", reviews.size()));
            
            // 카테고리 데이터 확인
            List<MainCategory> categories = mainCategoryRepository.findAll();
            status.put("categories", Map.of("count", categories.size()));
            
            // 티켓 데이터 확인
            List<Ticket> tickets = ticketRepository.findAll();
            status.put("tickets", Map.of("count", tickets.size()));
            
            // 스케줄 데이터 확인
            List<EventSchedule> schedules = eventScheduleRepository.findAll();
            status.put("schedules", Map.of("count", schedules.size()));
            
            // 사용자 데이터 확인
            List<Users> users = userRepository.findAll();
            status.put("users", Map.of("count", users.size()));
            
            // 예약 데이터 확인
            List<Reservation> reservations = reservationRepository.findAll();
            status.put("reservations", Map.of("count", reservations.size()));
            
            log.info("데이터베이스 상태: Events={}, Booths={}, Reviews={}, Categories={}, Tickets={}, Schedules={}, Users={}, Reservations={}", 
                events.size(), booths.size(), reviews.size(), categories.size(), tickets.size(), schedules.size(), users.size(), reservations.size());
                
        } catch (Exception e) {
            log.error("데이터베이스 상태 확인 오류", e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }

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
        
        // 5. 카테고리 데이터 로드 완전 비활성화 (검색 품질 이슈)
        // result.categoryResult = loadCategories();  // 완전 비활성화
        result.categoryResult = new LoadResult("Category", 0, 0, 0);
        log.info("카테고리 로딩 완전 비활성화: 예매/취소/환불 정책이 실제 이벤트 검색을 완전히 방해함");
        
        // 6. 사용자별 개인정보 데이터 로드 (예약정보, 티켓정보)
        result.userDataResult = loadUserData();
        
        log.info("종합 공개 데이터 RAG 로드 완료: {}", result.getSummary());
        
        return result;
    }
    
    /**
     * 사용자별 개인정보 데이터 로드 (예약정보, 티켓정보, 개인정보)
     */
    private LoadResult loadUserData() {
        log.info("사용자별 개인정보 데이터 로드 중...");
        List<Users> users = userRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (Users user : users) {
            try {
                // AI 봇 계정(999) 제외
                if (user.getUserId() == 999) {
                    continue;
                }
                
                Document document = buildUserDataDocument(user);
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                    log.debug("사용자 데이터 임베딩 성공: {} ({})", user.getUserId(), user.getName());
                } else {
                    failCount++;
                    log.warn("사용자 데이터 임베딩 실패: {} ({}) - 오류: {}", 
                        user.getUserId(), user.getName(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("사용자 데이터 로드 오류: {} ({}) - 예외: {}", 
                    user.getUserId(), user.getName(), e.getMessage(), e);
            }
        }
        
        log.info("사용자별 개인정보 데이터 로드 완료: 총 {}개 중 {}개 성공, {}개 실패", 
            users.size(), successCount, failCount);
        
        return new LoadResult("UserData", users.size(), successCount, failCount);
    }
    
    /**
     * 이벤트 데이터 로드
     */
    private LoadResult loadEvents() {
        log.info("이벤트 데이터 로드 중...");
        List<Event> events = eventRepository.findAll();
        log.info("데이터베이스에서 {}개의 이벤트를 찾았습니다", events.size());
        
        // 가져온 이벤트 ID들 로깅
        StringBuilder eventIds = new StringBuilder("가져온 이벤트 ID들: ");
        for (Event event : events) {
            eventIds.append(event.getEventId()).append("(").append(event.getTitleKr()).append("), ");
        }
        log.info(eventIds.toString());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Event event : events) {
            try {
                log.info("이벤트 처리 중: ID={}, TitleKr={}, TitleEng={}, Hidden={}", 
                    event.getEventId(), event.getTitleKr(), event.getTitleEng(), event.getHidden());
                
                EventDetail eventDetail = eventDetailRepository.findById(event.getEventId()).orElse(null);
                if (eventDetail == null) {
                    log.warn("이벤트 상세 정보 없음: eventId={}", event.getEventId());
                }
                
                Document document = buildEventDocument(event, eventDetail);
                log.debug("문서 생성 완료: docId={}, 제목={}, 내용 길이={}", 
                    document.getDocId(), document.getTitle(), document.getContent().length());
                
                DocumentIngestService.IngestResult result = documentIngestService.ingestDocument(document);
                if (result.isSuccess()) {
                    successCount++;
                    log.info("이벤트 임베딩 성공: {} ({})", event.getEventId(), event.getTitleKr());
                } else {
                    failCount++;
                    log.error("이벤트 임베딩 실패: {} ({}) - 오류: {}", 
                        event.getEventId(), event.getTitleKr(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failCount++;
                log.error("이벤트 로드 오류: {} ({}) - 예외: {}", 
                    event.getEventId(), event.getTitleKr(), e.getMessage(), e);
            }
        }
        
        log.info("이벤트 데이터 로드 완료: 총 {}개 중 {}개 성공, {}개 실패", 
            events.size(), successCount, failCount);
        
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
                log.error("부스 로드 오류: {} - {}", booth.getId(), e.getMessage(), e);
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
                log.error("부스 체험 로드 오류: {} - {}", experience.getExperienceId(), e.getMessage(), e);
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
                log.error("리뷰 로드 오류: {} - {}", review.getId(), e.getMessage(), e);
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
                // 캐시에서 SubCategory 가져오기 (N+1 문제 해결)
                List<SubCategory> subCategories = subCategoryCache.getOrDefault(
                    category.getGroupId(), new ArrayList<>());
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
                log.error("카테고리 로드 오류: {} - {}", category.getGroupId(), e.getMessage(), e);
            }
        }

        return new LoadResult("Category", categories.size(), successCount, failCount);
    }
    
    /**
     * Event + EventDetail을 RAG Document로 변환
     */
    private Document buildEventDocument(Event event, EventDetail eventDetail) {
        StringBuilder content = new StringBuilder();
        
        // 검색 키워드를 위한 제목들을 맨 앞에 배치
        content.append("=== 이벤트 정보 ===\n");
        content.append("이벤트 ID: ").append(event.getEventId()).append("\n");
        
        String mainTitle = null;
        if (event.getTitleKr() != null && !event.getTitleKr().isBlank()) {
            content.append("제목(한국어): ").append(event.getTitleKr()).append("\n");
            mainTitle = event.getTitleKr();
        }
        if (event.getTitleEng() != null && !event.getTitleEng().isBlank()) {
            content.append("제목(영어): ").append(event.getTitleEng()).append("\n");
            if (mainTitle == null) {
                mainTitle = event.getTitleEng();
            }
        }
        
        // 더 나은 검색을 위해 제목 키워드를 추가
        if (mainTitle != null) {
            content.append("검색 키워드: ");
            
            // 한글 제목 키워드
            if (event.getTitleKr() != null) {
                String[] krKeywords = event.getTitleKr().toLowerCase().split("\\s+");
                for (String keyword : krKeywords) {
                    if (keyword.length() > 1) {
                        content.append(keyword).append(" ");
                        // 부분 문자열도 추가 (송도 맥주축제 -> 송도, 맥주, 축제)
                        if (keyword.length() > 2) {
                            content.append(keyword.substring(0, 2)).append(" ");
                        }
                    }
                }
            }
            
            // 영문 제목 키워드
            if (event.getTitleEng() != null) {
                String[] engKeywords = event.getTitleEng().toLowerCase().split("\\s+");
                for (String keyword : engKeywords) {
                    if (keyword.length() > 1) {
                        content.append(keyword).append(" ");
                        // 부분 문자열도 추가
                        if (keyword.length() > 3) {
                            content.append(keyword.substring(0, 3)).append(" ");
                        }
                    }
                }
            }
            content.append("\n");
        }

        if (eventDetail != null) {
            // 일정 정보를 가장 먼저 표시 (중요 정보)
            content.append("\n=== 일정 정보 ===\n");
            if (eventDetail.getStartDate() != null) {
                content.append("시작일: ").append(eventDetail.getStartDate()).append("\n");
            }
            if (eventDetail.getEndDate() != null) {
                content.append("종료일: ").append(eventDetail.getEndDate()).append("\n");
            }
            if (eventDetail.getEventTime() != null) {
                content.append("소요 시간: ").append(eventDetail.getEventTime()).append("분\n");
            }
            
            // 카테고리 및 지역 정보
            if (eventDetail.getMainCategory() != null && eventDetail.getMainCategory().getGroupName() != null) {
                content.append("대분류: ").append(eventDetail.getMainCategory().getGroupName()).append("\n");
            }
            if (eventDetail.getSubCategory() != null && eventDetail.getSubCategory().getCategoryName() != null) {
                content.append("카테고리: ").append(eventDetail.getSubCategory().getCategoryName()).append("\n");
            }
            if (eventDetail.getRegionCode() != null && eventDetail.getRegionCode().getName() != null) {
                content.append("지역: ").append(eventDetail.getRegionCode().getName()).append("\n");
            }

            // 위치 정보 (중요하므로 앞쪽에 배치)
            content.append("\n=== 위치 정보 ===\n");
            if (eventDetail.getLocation() != null) {
                if (eventDetail.getLocation().getPlaceName() != null && !eventDetail.getLocation().getPlaceName().isBlank()) {
                    content.append("장소명: ").append(eventDetail.getLocation().getPlaceName()).append("\n");
                }
                if (eventDetail.getLocation().getAddress() != null && !eventDetail.getLocation().getAddress().isBlank()) {
                    content.append("주소: ").append(eventDetail.getLocation().getAddress()).append("\n");
                }
            }
            if (eventDetail.getLocationDetail() != null && !eventDetail.getLocationDetail().isBlank()) {
                content.append("장소 상세: ").append(eventDetail.getLocationDetail()).append("\n");
            }
            
            // 주최자 정보
            if (eventDetail.getHostName() != null && !eventDetail.getHostName().isBlank()) {
                content.append("주최자: ").append(eventDetail.getHostName()).append("\n");
            }
            
            // 연락처 및 웹사이트
            if (eventDetail.getContactInfo() != null && !eventDetail.getContactInfo().isBlank()) {
                content.append("연락처: ").append(eventDetail.getContactInfo()).append("\n");
            }
            if (eventDetail.getOfficialUrl() != null && !eventDetail.getOfficialUrl().isBlank()) {
                content.append("공식 웹사이트: ").append(eventDetail.getOfficialUrl()).append("\n");
            }
            
            // 이벤트 설명
            if (eventDetail.getBio() != null && !eventDetail.getBio().isBlank()) {
                content.append("소개: ").append(eventDetail.getBio()).append("\n");
            }
            
            // 상세 내용 (비활성화 - 예매/취소/환불 정책 등이 포함되어 검색 품질 저하)
            // if (eventDetail.getContent() != null && !eventDetail.getContent().isBlank()) {
            //     content.append("\n=== 이벤트 내용 ===\n");
            //     String cleanContent = removeHtmlTags(eventDetail.getContent());
            //     content.append(cleanContent).append("\n");
            // }
            
            // 정책 정보 (비활성화 - 예매/취소/환불 정책이 포함되어 검색 품질 저하)
            // if (eventDetail.getPolicy() != null && !eventDetail.getPolicy().isBlank()) {
            //     content.append("\n=== 이벤트 정책 ===\n");
            //     content.append(removeHtmlTags(eventDetail.getPolicy())).append("\n");
            // }
            
            // 추가 정보
            if (eventDetail.getReentryAllowed() != null) {
                content.append("재입장 허용: ").append(eventDetail.getReentryAllowed() ? "가능" : "불가능").append("\n");
            }
            if (eventDetail.getCheckInAllowed() != null && eventDetail.getCheckInAllowed()) {
                content.append("체크인 시스템: 사용\n");
            }
            if (eventDetail.getCheckOutAllowed() != null && eventDetail.getCheckOutAllowed()) {
                content.append("체크아웃 시스템: 사용\n");
            }
        }
        
        // 티켓 정보 추가
        try {
            List<Ticket> tickets = ticketRepository.findTicketsByEventId(event.getEventId());
            if (!tickets.isEmpty()) {
                content.append("\n=== 티켓 정보 ===\n");
                for (Ticket ticket : tickets) {
                    if (ticket.getName() != null) {
                        content.append("티켓명: ").append(ticket.getName());
                        if (ticket.getPrice() != null) {
                            content.append(" - 가격: ").append(String.format("%,d", ticket.getPrice())).append("원");
                        }
                        if (ticket.getStock() != null) {
                            content.append(" - 재고: ").append(ticket.getStock()).append("매");
                        }
                        content.append("\n");
                        
                        if (ticket.getDescription() != null && !ticket.getDescription().isBlank()) {
                            content.append("  설명: ").append(ticket.getDescription()).append("\n");
                        }
                        
                        if (ticket.getTicketAudienceType() != null && ticket.getTicketAudienceType().getName() != null) {
                            content.append("  대상: ").append(ticket.getTicketAudienceType().getName()).append("\n");
                        }
                        
                        if (ticket.getTicketSeatType() != null && ticket.getTicketSeatType().getName() != null) {
                            content.append("  좌석 유형: ").append(ticket.getTicketSeatType().getName()).append("\n");
                        }
                        
                        if (ticket.getMaxPurchase() != null) {
                            content.append("  최대 구매 수량: ").append(ticket.getMaxPurchase()).append("매\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("티켓 정보 수집 중 오류: eventId={} - {}", event.getEventId(), e.getMessage());
        }
        
        // 스케줄 정보 추가
        try {
            List<EventSchedule> schedules = eventScheduleRepository.findByEvent_EventId(event.getEventId());
            if (!schedules.isEmpty()) {
                content.append("\n=== 일정 스케줄 ===\n");
                for (EventSchedule schedule : schedules) {
                    content.append("날짜: ").append(schedule.getDate());
                    if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                        content.append(" 시간: ").append(schedule.getStartTime())
                               .append(" ~ ").append(schedule.getEndTime());
                    }
                    content.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("스케줄 정보 수집 중 오류: eventId={} - {}", event.getEventId(), e.getMessage());
        }
        
        String finalTitle = mainTitle != null ? mainTitle : ("이벤트 " + event.getEventId());
        String finalContent = content.toString();
        
        // 최소 내용 길이 확인
        if (finalContent.length() < 50) {
            log.warn("이벤트 문서 내용이 너무 짧음: eventId={}, 길이={}", event.getEventId(), finalContent.length());
        }
        
        return Document.builder()
            .docId("event_" + event.getEventId())
            .title(finalTitle)
            .content(finalContent)
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
        
        // 부스 배너 정보
        if (booth.getBoothBannerUrl() != null) {
            content.append("부스 배너: ").append(booth.getBoothBannerUrl()).append("\n");
        }
        
        // 부스 타입 정보
        if (booth.getBoothType() != null) {
            content.append("부스 타입: ").append(booth.getBoothType().getName()).append("\n");
        }
        
        // 부스 관리자 정보
        if (booth.getBoothAdmin() != null) {
            content.append("부스 관리자 ID: ").append(booth.getBoothAdmin().getUserId()).append("\n");
            if (booth.getBoothAdmin().getManagerName() != null) {
                content.append("부스 관리자명: ").append(booth.getBoothAdmin().getManagerName()).append("\n");
            }
            if (booth.getBoothAdmin().getEmail() != null) {
                content.append("부스 관리자 이메일: ").append(booth.getBoothAdmin().getEmail()).append("\n");
            }
        }
        
        // 이벤트 정보 (부스가 속한 이벤트)
        if (booth.getEvent() != null) {
            content.append("\n=== 소속 이벤트 ===\n");
            content.append("이벤트 ID: ").append(booth.getEvent().getEventId()).append("\n");
            if (booth.getEvent().getTitleKr() != null) {
                content.append("이벤트명: ").append(booth.getEvent().getTitleKr()).append("\n");
            }
            if (booth.getEvent().getTitleEng() != null) {
                content.append("이벤트명(영어): ").append(booth.getEvent().getTitleEng()).append("\n");
            }
        }
        
        // 부스 체험 정보 추가
        try {
            List<BoothExperience> experiences = boothExperienceRepository.findByBooth_Id(booth.getId());
            if (!experiences.isEmpty()) {
                content.append("\n=== 부스 체험 프로그램 ===\n");
                for (BoothExperience experience : experiences) {
                    if (experience.getTitle() != null) {
                        content.append("체험명: ").append(experience.getTitle()).append("\n");
                    }
                    if (experience.getDescription() != null) {
                        content.append("  설명: ").append(experience.getDescription()).append("\n");
                    }
                    if (experience.getDurationMinutes() != null) {
                        content.append("  소요시간: ").append(experience.getDurationMinutes()).append("분\n");
                    }
                    if (experience.getMaxCapacity() != null) {
                        content.append("  최대 참가자: ").append(experience.getMaxCapacity()).append("명\n");
                    }
                    content.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("부스 체험 정보 수집 중 오류: boothId={} - {}", booth.getId(), e.getMessage());
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
     * 사용자 개인정보를 RAG Document로 변환 (예약정보, 티켓정보, 개인정보)
     */
    private Document buildUserDataDocument(Users user) {
        StringBuilder content = new StringBuilder();
        
        content.append("=== 사용자 개인정보 ===\n");
        content.append("사용자 ID: ").append(user.getUserId()).append("\n");
        content.append("이름: ").append(user.getName()).append("\n");
        content.append("이메일: ").append(user.getEmail()).append("\n");
        content.append("닉네임: ").append(user.getNickname()).append("\n");
        
        if (user.getPhone() != null) {
            content.append("전화번호: ").append(user.getPhone()).append("\n");
        }
        if (user.getBirthday() != null) {
            content.append("생년월일: ").append(user.getBirthday()).append("\n");
        }
        if (user.getGender() != null) {
            content.append("성별: ").append(user.getGender()).append("\n");
        }
        if (user.getRoleCode() != null) {
            content.append("사용자 권한: ").append(user.getRoleCode().getName()).append("\n");
        }
        if (user.getCreatedAt() != null) {
            content.append("가입일: ").append(user.getCreatedAt()).append("\n");
        }
        
        // 예약 정보 조회
        try {
            List<Reservation> reservations = reservationRepository.findByUser_userId(user.getUserId());
            if (!reservations.isEmpty()) {
                content.append("\n=== 예약 내역 ===\n");
                for (Reservation reservation : reservations) {
                    content.append("예약 ID: ").append(reservation.getReservationId()).append("\n");
                    
                    if (reservation.getEvent() != null) {
                        content.append("  이벤트: ").append(reservation.getEvent().getTitleKr()).append("\n");
                        content.append("  이벤트 ID: ").append(reservation.getEvent().getEventId()).append("\n");
                    }
                    
                    if (reservation.getTicket() != null) {
                        content.append("  티켓명: ").append(reservation.getTicket().getName()).append("\n");
                        content.append("  티켓 가격: ").append(reservation.getTicket().getPrice()).append("원\n");
                    }
                    
                    if (reservation.getReservationStatusCode() != null) {
                        content.append("  예약 상태: ").append(reservation.getReservationStatusCode().getName()).append("\n");
                    }
                    content.append("  예약일: ").append(reservation.getCreatedAt()).append("\n");
                    
                    content.append("  수량: ").append(reservation.getQuantity()).append("매\n");
                    content.append("  가격: ").append(String.format("%,d", reservation.getPrice())).append("원\n");
                    
                    content.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("사용자 예약 정보 수집 중 오류: userId={} - {}", user.getUserId(), e.getMessage());
        }
        
        // 참석자 정보 조회 (다른 사람 예약에 참석자로 등록된 경우)
        try {
            // 이메일로 Attendee를 찾는 로직 (사용자의 이메일과 attendee 테이블의 이메일이 일치하는 경우)
            // 하지만 이는 복잡하므로 일단 생략하고 향후 필요시 추가
            // List<Attendee> attendees = attendeeRepository.findByEmail(user.getEmail());
            content.append("\n=== 참석자 정보 ===\n");
            content.append("다른 사용자 예약의 참석자로 등록된 내역은 별도 조회 가능\n");
        } catch (Exception e) {
            log.warn("사용자 참석자 정보 수집 중 오류: userId={} - {}", user.getUserId(), e.getMessage());
        }
        
        // 리뷰 작성 내역
        try {
            // Page 대신 List로 가져오기 위해 Pageable을 사용
            org.springframework.data.domain.Page<Review> reviewPage = reviewRepository.findByUser(user, 
                PageRequest.of(0, 100)); // 최대 100개 리뷰
            List<Review> userReviews = reviewPage.getContent();
            
            if (!userReviews.isEmpty()) {
                content.append("\n=== 작성한 리뷰 ===\n");
                for (Review review : userReviews) {
                    if (review.getReservation() != null && review.getReservation().getEvent() != null) {
                        content.append("이벤트: ").append(review.getReservation().getEvent().getTitleKr()).append("\n");
                    }
                    content.append("  평점: ").append(review.getStar()).append("점\n");
                    if (review.getComment() != null) {
                        content.append("  리뷰: ").append(review.getComment()).append("\n");
                    }
                    content.append("  작성일: ").append(review.getCreatedAt()).append("\n");
                    content.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("사용자 리뷰 정보 수집 중 오류: userId={} - {}", user.getUserId(), e.getMessage());
        }
        
        return Document.builder()
            .docId("user_" + user.getUserId())
            .title("사용자 " + user.getName() + "의 개인정보")
            .content(content.toString())
            .category("user_data")
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
        public LoadResult userDataResult;
        
        public String getSummary() {
            return String.format("Event: %s, Booth: %s, BoothExp: %s, Review: %s, Category: %s, UserData: %s",
                eventResult, boothResult, boothExperienceResult, reviewResult, categoryResult, userDataResult);
        }
        
        public int getTotalSuccessCount() {
            return eventResult.getSuccessCount() + 
                   boothResult.getSuccessCount() + 
                   boothExperienceResult.getSuccessCount() + 
                   reviewResult.getSuccessCount() + 
                   categoryResult.getSuccessCount() +
                   userDataResult.getSuccessCount();
        }
        
        public int getTotalFailCount() {
            return eventResult.getFailCount() + 
                   boothResult.getFailCount() + 
                   boothExperienceResult.getFailCount() + 
                   reviewResult.getFailCount() + 
                   categoryResult.getFailCount() +
                   userDataResult.getFailCount();
        }
    }
}