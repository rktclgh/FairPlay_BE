package com.fairing.fairplay.statistics.dto.sales;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RawSalesData {
    private Long eventId;
    private Long scheduleId;
    private String ticketName;
    private Long unitPrice;
    private Integer quantity;
    private Long amount;
    private String paymentStatus; // COMPLETED, CANCELLED, REFUNDED
}
