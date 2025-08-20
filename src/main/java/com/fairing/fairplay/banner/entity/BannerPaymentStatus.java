package com.fairing.fairplay.banner.entity;

public enum BannerPaymentStatus {
    PENDING("결제 대기"),
    PAID("결제 완료"), 
    CANCELLED("결제 취소");

    private final String description;

    BannerPaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}