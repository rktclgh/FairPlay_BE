package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    private Long paymentId;
    private Long eventId;               // 이벤트 ID
    private Long userId;                // 결제 요청자 ID
    
    // 결제 대상 관련 필드
    private String targetType;          // 결제 대상 타입 코드 (RESERVATION, BOOTH, AD)
    private Long targetId;              // 실제 결제 대상 ID
    
    private Integer quantity;           // 구매 수량
    private BigDecimal price;           // 개당 가격
    private String pgProvider;          // PG사
    private Integer paymentTypeCodeId;  // 결제 방법 코드
    private Integer paymentStatusCodeId;// 결제 상태 코드 (초기: PENDING)
    private String merchantUid;
    private String impUid;
    private BigDecimal amount;
    private BigDecimal refundRequestAmount;
    private String reason;

}
