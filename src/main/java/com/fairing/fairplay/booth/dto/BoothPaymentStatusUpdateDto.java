package com.fairing.fairplay.booth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothPaymentStatusUpdateDto {
    private String paymentStatusCode;  // 예: "PAID", "CANCELLED"
    private String adminComment;       // 선택: 사유 기록
}
