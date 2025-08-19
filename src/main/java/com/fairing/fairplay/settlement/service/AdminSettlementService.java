package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import com.fairing.fairplay.settlement.entity.AdminApprovalStatus;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.settlement.repository.SettlementCustomRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import com.fairing.fairplay.settlement.util.ExcelExporter;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettlementService {
    private final SettlementRepository settlementRepository;
    private final SettlementCustomRepository settlementCustomRepository;


    public Page<EventManagerSettlementListDto> getSettlementList(Pageable pageable) {
        return settlementCustomRepository.getAllApproveSettlement(pageable);
    }

    public Page<EventManagerSettlementListDto> searchSettlement(LocalDate startDate, LocalDate endDate, String keyword, String settlementStatus, String disputeStatus, Pageable pageable ) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        if (settlementStatus != null && "all".equalsIgnoreCase(settlementStatus)) {
            settlementStatus = "";
        }
        if (disputeStatus != null && "all".equalsIgnoreCase(disputeStatus)) {
            disputeStatus = "";
        }
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어(keyword)는 null 또는 빈 문자열일 수 없습니다.");
        }
        return settlementCustomRepository.searchSettlement(startDate,  endDate,  keyword, settlementStatus,  disputeStatus, pageable);
    }

    @Transactional
    public Settlement approveSettlement(Long settlementId, Long adminId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."));
        if (settlement.getAdminApprovalStatus() != AdminApprovalStatus.PENDING) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 처리된 정산입니다.");
            }
        settlement.setAdminApprovalStatus(AdminApprovalStatus.APPROVED);
        settlement.setApprovedBy(adminId);
        settlement.setApprovedAt(LocalDateTime.now());
        settlementRepository.save(settlement);
        return settlement;
    }

    @Transactional
    public Settlement  rejectSettlement(Long settlementId, Long adminId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."));

        if (settlement.getAdminApprovalStatus() != AdminApprovalStatus.PENDING) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 처리된 정산입니다.");
        }
        settlement.setAdminApprovalStatus(AdminApprovalStatus.REJECTED);
        settlement.setApprovedBy(adminId);
        settlement.setApprovedAt(LocalDateTime.now());
        settlementRepository.save(settlement);
        return settlement;
    }

    public byte[] exportSettlements(LocalDate startDate, LocalDate endDate, String keyword, String settlementStatus, String disputeStatus) throws IOException {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
            }
        if (settlementStatus != null && "all".equalsIgnoreCase(settlementStatus)) {
            settlementStatus = "";
            }
        if (disputeStatus != null && "all".equalsIgnoreCase(disputeStatus)) {
            disputeStatus = "";
            }
        Pageable pageable = Pageable.unpaged(); // 전체 조회
        Page<EventManagerSettlementListDto> page = settlementCustomRepository.searchSettlement(
                startDate, endDate, keyword, settlementStatus, disputeStatus, pageable
        );

        return ExcelExporter.exportSettlementList(page.getContent());
    }

}
