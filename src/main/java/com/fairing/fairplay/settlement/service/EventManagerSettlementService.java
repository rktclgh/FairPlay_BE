package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.settlement.dto.*;
import com.fairing.fairplay.settlement.entitiy.Settlement;
import com.fairing.fairplay.settlement.entitiy.SettlementAccount;
import com.fairing.fairplay.settlement.entitiy.SettlementRequestStatus;
import com.fairing.fairplay.settlement.repository.SettlementAccountRepository;
import com.fairing.fairplay.settlement.repository.SettlementCustomRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventManagerSettlementService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private final SettlementRepository settlementRepository;
    private final SettlementAccountRepository settlementAccountRepository;
    private final SettlementCustomRepository settlementCustomRepository;


    public Page<EventManagerSettlementListDto> getAllSettlement(Long memberId, Pageable pageable) {

        Page<EventManagerSettlementListDto> settlementPageList = settlementCustomRepository.findAllByAdminUserId(memberId,pageable)
                .map(r-> EventManagerSettlementListDto.builder()
                        .settlementId(r.getSettlementId())
                        .eventId(r.getEvent().getEventId())
                        .eventTitle(r.getEventTitle())
                        .finalAmount(r.getFinalAmount())
                        .disputeStatus(r.getDisputeStatus())
                        .adminApprovalStatus(r.getAdminApprovalStatus())
                        .transferStatus(r.getTransStatus())
                        .build());

        return settlementPageList;
    }

    public EventManagerDetailSettlementDto getDetailSettlement(Long settlementId, Long userId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));
        Optional<SettlementAccount> accountInfo = settlementAccountRepository.findBySettlement_SettlementId(settlementId).or(() -> Optional.of(new SettlementAccount()));

        // 소유자 검증
        if (!settlement.getEvent().getManager().getUserId().equals(userId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        List<SettlementAggregationRevenueDto> revenueDatail = settlement.getRevenueDetails().stream().map(r->SettlementAggregationRevenueDto.builder()
                .revenueTypeAmount(r.getAmount())
                .revenueType(r.getRevenueType())
                .build())
                        .toList();


        return EventManagerDetailSettlementDto.builder()
                .settlementId(settlement.getSettlementId())
                .eventId(settlement.getEvent().getEventId())
                .eventTitle(settlement.getEventTitle())
                .totalAmount(settlement.getTotalAmount())
                .feeAmount(settlement.getFeeAmount())
                .finalAmount(settlement.getFinalAmount())
                .accountNumber(accountInfo.map(SettlementAccount::getAccountNumber).orElse(""))
                .holderName(accountInfo.map(SettlementAccount::getHolderName).orElse(""))
                .bankName(accountInfo.map(SettlementAccount::getBankName).orElse(""))
                .scheduledDate(settlement.getScheduledDate())
                .completedDate(settlement.getCompletedDate())
                .transferStatus(settlement.getTransStatus())
                .disputeStatus(settlement.getDisputeStatus())
                .adminApprovalStatus(settlement.getAdminApprovalStatus())
                .revenueDetail(revenueDatail)
                .build();
    }

    @Transactional
    public Long registerAccount(Long settlementId, AccountRequestDto accountRequestDto, Long userId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));

        // 소유자 검증
        if (!settlement.getEvent().getManager().getUserId().equals(userId)) {
            throw new AccessDeniedException("권한이 없습니다.");
            }
        SettlementAccount settlementAccount = settlementAccountRepository
                .findBySettlement_SettlementId(settlementId)
                .orElseGet(() -> SettlementAccount.builder().settlement(settlement).build());

        settlementAccount.setAccountNumber(accountRequestDto.getAccountNumber());
        settlementAccount.setHolderName(accountRequestDto.getHolderName());
        settlementAccount.setBankName(accountRequestDto.getBankName());
        SettlementAccount saved = settlementAccountRepository.save(settlementAccount);

        return saved.getAccountId();
    }
    @Transactional
    public ApproveEventManagerSettlementDto approveEventManagerSettlement (Long settlementId, Long userId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));

        if (!settlement.getEvent().getManager().getUserId().equals(userId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        // 상태 전이 검증
        if (settlement.getSettlementRequestStatus() != SettlementRequestStatus.PENDING) {
            throw new IllegalStateException("잘못된 상태 전이: " + settlement.getSettlementRequestStatus());
                }
        settlement.setSettlementRequestStatus(SettlementRequestStatus.REQUESTED);
        settlementRepository.save(settlement);

        return  ApproveEventManagerSettlementDto.builder()
                .settlementId(settlementId)
                .settlementRequestStatus(SettlementRequestStatus.REQUESTED)
                .build();
    }





}
