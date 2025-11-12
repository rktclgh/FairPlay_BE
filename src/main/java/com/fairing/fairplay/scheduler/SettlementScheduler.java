package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.settlement.service.SettlementBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final EventRepository eventRepository;
    private final SettlementBatchService settlementBatchService;

    // 매일 새벽 1시에 실행 (cron = 초 분 시 * * ?)
    @Scheduled(cron = "0 0 1 * * ?")
    public void processAutomaticSettlement() {
        LocalDate targetDate = LocalDate.now().minusDays(7);

        log.info("자동 정산 배치 작업 시작 - 대상일: {}", targetDate);

        try {
            // 7일 전에 종료된 행사 조회
            List<Event> events = eventRepository.findAllByEventDetail_EndDateLessThanEqual(targetDate);
            log.info("정산 대상 이벤트 수: {}", events.size());

            int successCount = 0;
            int failCount = 0;

            // 각 이벤트를 독립적인 트랜잭션으로 처리
            for (Event event : events) {
                try {
                    settlementBatchService.processSettlementForEvent(event);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("이벤트 정산 실패: eventId={}, eventTitle={}",
                            event.getEventId(), event.getTitleKr(), e);
                    // 개별 실패는 전체 배치를 중단시키지 않음
                }
            }

            log.info("자동 정산 배치 작업 완료 - 성공: {}, 실패: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("자동 정산 배치 작업 실패", e);
        }
    }
}
