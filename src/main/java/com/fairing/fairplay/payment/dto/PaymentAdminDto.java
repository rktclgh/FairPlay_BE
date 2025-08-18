package com.fairing.fairplay.payment.dto;

import com.fairing.fairplay.payment.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAdminDto {

    private Long paymentId;
    private String merchantUid;
    private String impUid;

    // 행사 정보
    private Long eventId;
    private String eventName;

    // 결제 대상 정보
    private String paymentTargetType; // RESERVATION, BOOTH, AD
    private String paymentTargetName; // 예약, 부스, 광고
    private Long targetId;

    // 구매자 정보
    private Long userId;
    private String buyerName;
    private String buyerEmail;

    // 결제 정보
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal refundedAmount;

    // 결제 상태
    private String paymentStatusCode;
    private String paymentStatusName;
    private String paymentTypeCode;
    private String paymentTypeName;

    // 결제 일시
    private LocalDateTime requestedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    // PG 정보
    private String pgProvider;

    /**
     * Payment 엔티티를 PaymentAdminDto로 변환
     */
    public static PaymentAdminDto fromEntity(Payment payment) {
        return PaymentAdminDto.builder()
                .paymentId(payment.getPaymentId())
                .merchantUid(payment.getMerchantUid())
                .impUid(payment.getImpUid())
                
                // 행사 정보
                .eventId(payment.getEvent() != null ? payment.getEvent().getEventId() : null)
                .eventName(payment.getEvent() != null ? payment.getEvent().getTitleKr() : null)
                
                // 결제 대상 정보
                .paymentTargetType(payment.getPaymentTargetType().getPaymentTargetCode())
                .paymentTargetName(payment.getPaymentTargetType().getPaymentTargetName())
                .targetId(payment.getTargetId())
                
                // 구매자 정보
                .userId(payment.getUser().getUserId())
                .buyerName(payment.getUser().getName())
                .buyerEmail(payment.getUser().getEmail())
                
                // 결제 정보
                .quantity(payment.getQuantity())
                .price(payment.getPrice())
                .amount(payment.getAmount())
                .refundedAmount(payment.getRefundedAmount())
                
                // 결제 상태
                .paymentStatusCode(payment.getPaymentStatusCode().getCode())
                .paymentStatusName(payment.getPaymentStatusCode().getName())
                .paymentTypeCode(payment.getPaymentTypeCode().getCode())
                .paymentTypeName(payment.getPaymentTypeCode().getName())
                
                // 결제 일시
                .requestedAt(payment.getRequestedAt())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                
                // PG 정보
                .pgProvider(payment.getPgProvider())
                .build();
    }

    /**
     * Payment 엔티티 리스트를 PaymentAdminDto 리스트로 변환
     */
    public static List<PaymentAdminDto> fromEntityList(List<Payment> payments) {
        return payments.stream()
                .map(PaymentAdminDto::fromEntity)
                .collect(Collectors.toList());
    }
}