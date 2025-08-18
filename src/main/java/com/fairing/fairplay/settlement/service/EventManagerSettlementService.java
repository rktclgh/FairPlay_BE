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
import org.springframework.stereotype.Service;
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
                        .finalAmount(r.getTotalAmount())
                        .disputeStatus(r.getDisputeStatus())
                        .adminApprovalStatus(r.getAdminApprovalStatus())
                        .transferStatus(r.getTransStatus())
                        .build());

        return settlementPageList;
    }

    public EventManagerDetailSettlementDto getDetailSettlement(Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));
        Optional<SettlementAccount> accountInfo = settlementAccountRepository.findBySettlement_SettlementId(settlementId).or(() -> Optional.of(new SettlementAccount()));

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


    public Long registerAccount(Long settlementId, AccountRequestDto accountRequestDto) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));

        SettlementAccount settlementAccount = SettlementAccount.builder()
                .accountNumber(accountRequestDto.getAccountNumber())
                .holderName(accountRequestDto.getHolderName())
                .bankName(accountRequestDto.getBankName())
                .settlement(settlement)
                .build();

        settlementAccountRepository.save(settlementAccount);

        return settlementAccount.getAccountId();
    }

    public ApproveEventManagerSettlementDto approveEventManagerSettlement (Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 정보를 찾을 수 없습니다."));


        settlement.setSettlementRequestStatus(SettlementRequestStatus.REQUESTED);
        settlementRepository.save(settlement);

        return  ApproveEventManagerSettlementDto.builder()
                .SettlementId(settlementId)
                .settlementRequestStatus(SettlementRequestStatus.REQUESTED)
                .build();
    }





}
