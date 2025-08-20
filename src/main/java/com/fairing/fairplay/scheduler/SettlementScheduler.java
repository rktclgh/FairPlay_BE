package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.settlement.dto.SettlementAggregationRevenueDto;
import com.fairing.fairplay.settlement.entity.*;
import com.fairing.fairplay.settlement.repository.SettlementCustomRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final EventRepository eventRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementCustomRepository settlementCustomRepository;

    // 매일 새벽 1시에 실행 (cron = 초 분 시 * * ?)
    @Scheduled(cron = "0 0 1 * * ?")
    public void processAutomaticSettlement() {
        LocalDate targetDate = LocalDate.now().minusDays(7);


            // 7일 전에 종료된 행사 조회
            List<Event> events = eventRepository.findAllByEventDetail_EndDateLessThanEqual(targetDate);


            for (Event event : events) {
               try{ // 이미 정산된 행사인지 확인
                boolean alreadySettled = settlementRepository.findByEvent_EventId(event.getEventId()).isPresent();
                if (alreadySettled) continue;

                // 👉 여기서 실제 수익 집계 로직을 넣어야 함 (예매, 광고 등)
                BigDecimal totalAmount = settlementCustomRepository.aggregatedCalculate(event.getEventId()).getTotalAmount();
                   BigDecimal feeAmount = totalAmount
                                                   .multiply(new BigDecimal("0.05")) // 수수료 5%
                                                   .setScale(2, java.math.RoundingMode.HALF_UP);// 수수료 5%


                // 수익 상세
                BigDecimal ticketRevenue = settlementCustomRepository.aggregatedRevenueCalculate(event.getEventId()).stream().filter(
                                r -> "RESERVATION".equals(r.getRevenueType()))
                        .map(SettlementAggregationRevenueDto::getRevenueTypeAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal boothRevenue = settlementCustomRepository.aggregatedRevenueCalculate(event.getEventId()).stream().filter(
                                r -> "BOOTH".equals(r.getRevenueType()))
                        .map(SettlementAggregationRevenueDto::getRevenueTypeAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);


                // 최종 정산 금액
                   BigDecimal finalAmount = totalAmount
                           .subtract(feeAmount)
                           .subtract(boothRevenue)
                           .setScale(2, java.math.RoundingMode.HALF_UP);

                Settlement settlement = Settlement.builder()
                        .event(event)
                        .eventTitle(event.getTitleKr())
                        .totalAmount(totalAmount)
                        .feeAmount(feeAmount)
                        .finalAmount(finalAmount)
                        .adminApprovalStatus(AdminApprovalStatus.PENDING)
                        .disputeStatus(DisputeStatus.NONE)
                        .settlementRequestStatus(SettlementRequestStatus.PENDING)
                        .transStatus(TransferStatus.PENDING)
                        .scheduledDate(LocalDate.now())
                        .completedDate(null)
                        .build();

                SettlementRevenueDetail d1 = SettlementRevenueDetail.builder()
                        .settlement(settlement)
                        .revenueType("예매")
                        .amount(ticketRevenue)
                        .createdAt(LocalDateTime.now())
                        .build();

                SettlementRevenueDetail d2 = SettlementRevenueDetail.builder()
                        .settlement(settlement)
                        .revenueType("부스")
                        .amount(boothRevenue)
                        .createdAt(LocalDateTime.now())
                        .build();

                settlement.getRevenueDetails().add(d1);
                settlement.getRevenueDetails().add(d2);

                settlementRepository.save(settlement);

                log.info("자동 정산 완료: {}", event.getEventId());

            } catch (RuntimeException e) {
            log.error("자동 정산 실패", e);
            }
        }
    }
}
