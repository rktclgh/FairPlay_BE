package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventStatusCode;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.EventStatusCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatusScheduler {

    private final EventRepository eventRepository;
    private final EventStatusCodeRepository statusCodeRepository;
    private final EventVersionService eventVersionService;
    private static final String NOT_FOUND_STATUS = "해당 상태 코드 없음";

    // 시스템 자동 변경인 경우 사용할 관리자 ID (예: -1L 또는 0L)
    private static final Long SYSTEM_MANAGER_ID = -1L;

    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 00:00에 실행
    public void updateEventStatuses() {
        LocalDate today = LocalDate.now();

        log.info("이벤트 상태 자동 업데이트 배치 시작 - 기준일: {}", today);

        try {
            EventStatusCode upcoming = statusCodeRepository.findByCode("UPCOMING")
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
            EventStatusCode ongoing = statusCodeRepository.findByCode("ONGOING")
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
            EventStatusCode ended = statusCodeRepository.findByCode("ENDED")
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));

            // 전체 이벤트 조회
            List<Event> events = eventRepository.findAll();
            log.info("전체 이벤트 수: {}", events.size());

            int updatedCount = 0;

            // 상태 변경이 필요한 이벤트만 필터링 및 처리
            for (Event event : events) {
                try {
                    EventStatusCode currentStatus = event.getStatusCode();
                    EventStatusCode newStatus = determineStatus(today, event.getEventDetail(), upcoming, ongoing, ended);

                    if (!currentStatus.getEventStatusCodeId().equals(newStatus.getEventStatusCodeId())) {
                        // 각 이벤트를 독립적인 트랜잭션으로 처리
                        updateEventStatus(event, newStatus);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    log.error("이벤트 상태 변경 실패: eventId={}, eventTitle={}",
                            event.getEventId(), event.getTitleKr(), e);
                    // 개별 실패는 전체 배치를 중단시키지 않음
                }
            }

            log.info("이벤트 상태 자동 업데이트 배치 완료 - 변경된 이벤트 수: {}", updatedCount);

        } catch (Exception e) {
            log.error("이벤트 상태 자동 업데이트 배치 실패", e);
        }
    }

    /**
     * 단일 이벤트의 상태를 업데이트 (독립적인 트랜잭션)
     */
    @Transactional(timeout = 10)
    public void updateEventStatus(Event event, EventStatusCode newStatus) {
        String oldStatusCode = event.getStatusCode().getCode();
        event.setStatusCode(newStatus);

        // 상태 변경 후 버전 생성
        eventVersionService.createEventVersion(event, SYSTEM_MANAGER_ID);

        log.info("eventId={} 상태 변경: {} → {}",
                event.getEventId(),
                oldStatusCode,
                newStatus.getCode());
    }

    private EventStatusCode determineStatus(LocalDate today, EventDetail eventDetail,
                                            EventStatusCode upcoming,
                                            EventStatusCode ongoing,
                                            EventStatusCode ended) {

        if (eventDetail == null) {
            // EventDetail이 없는 경우
            throw new CustomException(HttpStatus.NOT_FOUND, "행사 상세 정보가 존재하지 않습니다. 먼저 상세 정보를 등록하세요.");
        }

        if (today.isBefore(eventDetail.getStartDate())) {
            return upcoming;
        } else if (!today.isAfter(eventDetail.getEndDate())) {
            return ongoing;
        } else {
            return ended;
        }
    }

}
