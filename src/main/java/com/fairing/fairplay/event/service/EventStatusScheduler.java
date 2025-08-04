package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventStatusCode;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.EventStatusCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 00:00에 실행
    public void updateEventStatuses() {
        LocalDate today = LocalDate.now();

        EventStatusCode upcoming = statusCodeRepository.findByCode("UPCOMING")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
        EventStatusCode ongoing = statusCodeRepository.findByCode("ONGOING")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));
        EventStatusCode ended = statusCodeRepository.findByCode("ENDED")
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, NOT_FOUND_STATUS));

        // 전체 이벤트 조회
        List<Event> events = eventRepository.findAll();

        // 상태 변경이 필요한 이벤트만 필터링
        events.stream()
                .map(event -> {
                    EventStatusCode currentStatus = event.getStatusCode();
                    EventStatusCode newStatus = determineStatus(today, event.getEventDetail(), upcoming, ongoing, ended);

                    if (!currentStatus.getEventStatusCodeId().equals(newStatus.getEventStatusCodeId())) {
                        // 상태 변경 필요 → 새로운 상태 세팅
                        event.setStatusCode(newStatus);
                        return event;
                    } else {
                        // 변경 없음
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(event -> {
                    // 상태 변경 후 버전 생성
                    eventVersionService.createEventVersion(event, SYSTEM_MANAGER_ID);
                    log.info("eventId={} 상태 변경: {} → {}",
                            event.getEventId(),
                            event.getStatusCode().getCode(),
                            event.getStatusCode().getCode());
                });
    }

    private EventStatusCode determineStatus(LocalDate today, EventDetail eventDetail,
                                            EventStatusCode upcoming,
                                            EventStatusCode ongoing,
                                            EventStatusCode ended) {

        if (today.isBefore(eventDetail.getStartDate())) {
            return upcoming;
        } else if (!today.isAfter(eventDetail.getEndDate())) {
            return ongoing;
        } else {
            return ended;
        }
    }

}
