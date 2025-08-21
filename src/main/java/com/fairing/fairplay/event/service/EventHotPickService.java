package com.fairing.fairplay.event.service;

import com.fairing.fairplay.banner.dto.HotPickDto;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventHotPickService {
    
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<HotPickDto> getHotPicksByReservation(int limit) {
        log.info("핫픽 조회 시작 - 일주일간 예약 수량 기준, limit: {}", limit);
        
        try {
            // 1. 일주일간 예약 수량 상위 이벤트 조회 (CONFIRMED 상태만)
            List<Object[]> weeklyRanking = reservationRepository
                    .findEventBookingQuantitiesLastWeek(List.of("CONFIRMED"), limit);
            
            log.info("일주일간 예약 순위 조회 결과: {}건", weeklyRanking.size());
            
            List<HotPickDto> hotPicks = new ArrayList<>();
            
            for (Object[] row : weeklyRanking) {
                Long eventId = ((Number) row[0]).longValue();
                Long bookedQty = ((Number) row[1]).longValue();
                
                log.info("핫픽 후보 - EventId: {}, 일주일간 예약 수량: {}매", eventId, bookedQty);
                
                // 2. 이벤트 정보 조회
                Event event = eventRepository.findById(eventId).orElse(null);
                if (event == null) {
                    log.warn("이벤트 {}를 찾을 수 없음", eventId);
                    continue;
                }
                
                // 3. HotPickDto 생성
                HotPickDto hotPick = HotPickDto.builder()
                        .id(eventId)
                        .title(event.getTitleKr())
                        .date(formatEventDate(event))
                        .location(event.getEventDetail() != null && event.getEventDetail().getLocation() != null 
                                ? event.getEventDetail().getLocation().getBuildingName() 
                                : "장소 미정")
                        .category(event.getEventDetail() != null && event.getEventDetail().getMainCategory() != null 
                                ? event.getEventDetail().getMainCategory().getGroupName() 
                                : "기타")
                        .image(event.getEventDetail() != null ? event.getEventDetail().getThumbnailUrl() : null)
                        .build();
                
                hotPicks.add(hotPick);
                log.info("핫픽 생성 완료 - {}, 예약 수량: {}매", event.getTitleKr(), bookedQty);
            }
            
            log.info("핫픽 조회 완료 - 총 {}건 반환", hotPicks.size());
            return hotPicks;
            
        } catch (Exception e) {
            log.error("핫픽 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    private String formatEventDate(Event event) {
        if (event.getEventDetail() == null) return "";
        
        String startDate = event.getEventDetail().getStartDate() != null 
                ? event.getEventDetail().getStartDate().toString() : "";
        String endDate = event.getEventDetail().getEndDate() != null 
                ? event.getEventDetail().getEndDate().toString() : "";
        
        if (startDate.equals(endDate)) {
            return startDate;
        } else {
            return startDate + " ~ " + endDate;
        }
    }
}