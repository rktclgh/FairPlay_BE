package com.fairing.fairplay.settlement.dto;

import com.fairing.fairplay.settlement.entity.SettlementRequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class ApproveEventManagerSettlementDto {
    private Long settlementId;
    private SettlementRequestStatus settlementRequestStatus;
}
