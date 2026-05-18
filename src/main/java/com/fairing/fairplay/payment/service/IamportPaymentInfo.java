package com.fairing.fairplay.payment.service;

import java.math.BigDecimal;

public record IamportPaymentInfo(
        String impUid,
        String merchantUid,
        String status,
        BigDecimal amount
) {
}
