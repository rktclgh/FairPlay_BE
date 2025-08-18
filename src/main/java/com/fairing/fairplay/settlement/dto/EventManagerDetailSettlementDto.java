package com.fairing.fairplay.settlement.dto;

import com.fairing.fairplay.settlement.entitiy.AdminApprovalStatus;
import com.fairing.fairplay.settlement.entitiy.DisputeStatus;
import com.fairing.fairplay.settlement.entitiy.TransferStatus;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class EventManagerDetailSettlementDto {
    private Long settlementId;
    private Long eventId;
    private String eventTitle;
    private BigDecimal finalAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private List<SettlementAggregationRevenueDto> revenueDetail;
    private DisputeStatus disputeStatus;
    private AdminApprovalStatus adminApprovalStatus;
    private TransferStatus transferStatus;
    private String bankName; // 은행명
    private String accountNumber; // 계좌번호
    private String holderName; // 예금주명
    private LocalDate scheduledDate; // 송금 예정일
    private LocalDate completedDate; // 송금 완료일
}
