package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundApprovalDto {
    
    private String action;              // "APPROVE" 또는 "REJECT"
    private String adminComment;        // 관리자 코멘트/승인 사유
    private BigDecimal refundAmount;    // 실제 환불 금액 (부분 수정 가능)
    
    // 승인 시에만 필요
    private Boolean processImmediately; // 즉시 PG사 환불 처리 여부
}