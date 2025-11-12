package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.settlement.dto.SettlementAggregationRevenueDto;
import com.fairing.fairplay.settlement.entity.*;
import com.fairing.fairplay.settlement.repository.SettlementCustomRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 배치 처리를 담당하는 서비스
 * 각 이벤트별로 독립적인 트랜잭션으로 처리하여 커넥션 풀 고갈 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final SettlementRepository settlementRepository;
    private final SettlementCustomRepository settlementCustomRepository;

    /**
     * 단일 이벤트에 대한 정산 처리 (독립적인 트랜잭션)
     * @param event 정산 대상 이벤트
     */
    @Transactional(timeout = 30)  // 30초 타임아웃 설정
    public void processSettlementForEvent(Event event) {
        try {
            // 이미 정산된 행사인지 확인
            boolean alreadySettled = settlementRepository.findByEvent_EventId(event.getEventId()).isPresent();
            if (alreadySettled) {
                log.debug("이미 정산 완료된 이벤트: eventId={}", event.getEventId());
                return;
            }

            // 실제 수익 집계
            BigDecimal totalAmount = settlementCustomRepository.aggregatedCalculate(event.getEventId()).getTotalAmount();
            BigDecimal feeAmount = totalAmount
                    .multiply(new BigDecimal("0.05")) // 수수료 5%
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // 수익 상세
            BigDecimal ticketRevenue = settlementCustomRepository.aggregatedRevenueCalculate(event.getEventId()).stream()
                    .filter(r -> "RESERVATION".equals(r.getRevenueType()))
                    .map(SettlementAggregationRevenueDto::getRevenueTypeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal boothRevenue = settlementCustomRepository.aggregatedRevenueCalculate(event.getEventId()).stream()
                    .filter(r -> "BOOTH".equals(r.getRevenueType()))
                    .map(SettlementAggregationRevenueDto::getRevenueTypeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 최종 정산 금액
            BigDecimal finalAmount = totalAmount
                    .subtract(feeAmount)
                    .subtract(boothRevenue)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // Settlement 엔티티 생성
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

            // Revenue Details 추가
            SettlementRevenueDetail ticketDetail = SettlementRevenueDetail.builder()
                    .settlement(settlement)
                    .revenueType("예매")
                    .amount(ticketRevenue)
                    .createdAt(LocalDateTime.now())
                    .build();

            SettlementRevenueDetail boothDetail = SettlementRevenueDetail.builder()
                    .settlement(settlement)
                    .revenueType("부스")
                    .amount(boothRevenue)
                    .createdAt(LocalDateTime.now())
                    .build();

            settlement.getRevenueDetails().add(ticketDetail);
            settlement.getRevenueDetails().add(boothDetail);

            // 저장
            settlementRepository.save(settlement);

            log.info("자동 정산 완료: eventId={}, eventTitle={}, finalAmount={}",
                    event.getEventId(), event.getTitleKr(), finalAmount);

        } catch (Exception e) {
            log.error("자동 정산 실패: eventId={}, eventTitle={}",
                    event.getEventId(), event.getTitleKr(), e);
            throw e;  // 트랜잭션 롤백을 위해 예외 재발생
        }
    }
}
