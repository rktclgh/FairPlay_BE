package com.fairing.fairplay.banner.dto;

import com.fairing.fairplay.banner.entity.NewBannerApplication.ApplicationStatus;
import com.fairing.fairplay.banner.entity.NewBannerApplication.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BannerApplicationDto {
    private Long id;
    private String eventTitle;
    private String bannerType;
    private String bannerTypeName;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String startDate;
    private String endDate;
    private Integer totalAmount;
    private ApplicationStatus applicationStatus;
    private PaymentStatus paymentStatus;
    private String combinedStatus;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private String adminComment;
    private Boolean canCancel;
    private Boolean canPay;
    private String paymentUrl;
    private List<SlotInfo> slots;

    @Data
    @Builder
    public static class SlotInfo {
        private String slotDate;
        private Integer priority;
        private Integer price;
    }
}