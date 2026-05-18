package com.fairing.fairplay.payment.service;

import java.math.BigDecimal;

public record ReservationPaymentIntent(
        Long eventId,
        Long scheduleId,
        Long ticketId,
        Integer quantity,
        BigDecimal amount,
        String targetType,
        Long userId,
        String merchantUid
) {
}
