package com.fairing.fairplay.settlement.dto;

import com.fairing.fairplay.settlement.entity.AdminApprovalStatus;
import com.fairing.fairplay.settlement.entity.DisputeStatus;
import com.fairing.fairplay.settlement.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class EventManagerSettlementListDto {

    private Long settlementId;
    private Long eventId;
    private String eventTitle;
    private BigDecimal finalAmount;
    private DisputeStatus disputeStatus;
    private AdminApprovalStatus adminApprovalStatus;
    private TransferStatus transferStatus;
    private LocalDate createdAt;
}
