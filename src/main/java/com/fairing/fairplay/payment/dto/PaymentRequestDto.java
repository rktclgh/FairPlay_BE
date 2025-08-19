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
    private Long eventId;               // 이벤트 ID (선택적 - 광고 결제 등은 null 가능)
    private Long userId;                // 결제 요청자 ID
    
    // 결제 대상 관련 필드
    private String paymentTargetType;          // 결제 대상 타입 코드 (RESERVATION, BOOTH, AD)
    private Long targetId;              // 실제 결제 대상 ID
    
    private Integer quantity;           // 구매 수량
    private BigDecimal price;           // 개당 가격
    private String pgProvider;          // PG사
    private Integer paymentTypeCodeId;  // 결제 방법 코드
    private Integer paymentStatusCodeId;// 결제 상태 코드 (초기: PENDING)
    private String merchantUid;
    private String impUid;
    private BigDecimal amount;
    private String applyNum;                // 카드 승인번호
    private BigDecimal refundRequestAmount;
    private String reason;
    
    // 예매 생성을 위한 추가 정보
    private Long scheduleId;            // 이벤트 스케줄 ID
    
    // 이메일에서 접근하는 경우 사용자 식별을 위한 필드
    private String contactEmail;        // 연락처 이메일 (부스 신청자 이메일)

}
