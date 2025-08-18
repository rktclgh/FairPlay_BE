package com.fairing.fairplay.payment.dto;

import com.fairing.fairplay.payment.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private Long paymentId;
    private Long eventId;
    private String eventName;           // 이벤트명
    private LocalDate eventStartDate; // 이벤트 시작일
    private LocalDate eventEndDate;   // 이벤트 종료일
    private Long userId;
    
    // 결제 대상 관련 필드
    private String targetType;          // 결제 대상 타입 코드 (RESERVATION, BOOTH, AD)
    private String targetTypeName;      // 결제 대상 타입 이름 (예약, 부스, 광고)
    private Long targetId;              // 실제 결제 대상 ID
    
    private String merchantUid;
    private String impUid;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal refundedAmount;
    private BigDecimal amount;
    private String pgProvider;
    private Integer paymentTypeCodeId;
    private Integer paymentStatusCodeId;
    private LocalDateTime requestedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    public static PaymentResponseDto fromEntity(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .eventId(payment.getEvent() != null ? payment.getEvent().getEventId() : null)
                .eventName(payment.getEvent() != null ? payment.getEvent().getTitleKr() : null)
                .eventStartDate(payment.getEvent() != null ? payment.getEvent().getEventDetail().getStartDate() : null)
                .eventEndDate(payment.getEvent() != null ? payment.getEvent().getEventDetail().getEndDate() : null)
                .userId(payment.getUser().getUserId())
                .targetType(payment.getPaymentTargetType().getPaymentTargetCode())
                .targetTypeName(payment.getPaymentTargetType().getPaymentTargetName())
                .targetId(payment.getTargetId())
                .merchantUid(payment.getMerchantUid())
                .impUid(payment.getImpUid())
                .quantity(payment.getQuantity())
                .price(payment.getPrice())
                .refundedAmount(payment.getRefundedAmount())
                .amount(payment.getAmount())
                .pgProvider(payment.getPgProvider())
                .paymentTypeCodeId(payment.getPaymentTypeCode().getPaymentTypeCodeId())
                .paymentStatusCodeId(payment.getPaymentStatusCode().getPaymentStatusCodeId())
                .requestedAt(payment.getRequestedAt())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    public static List<PaymentResponseDto> fromEntityList(List<Payment> payments){
        List<PaymentResponseDto> paymentList = new ArrayList<>();

        for(Payment p : payments){
            paymentList.add(PaymentResponseDto.fromEntity(p));
        }

        return paymentList;
    }
}
