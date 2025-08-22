package com.fairing.fairplay.banner.entity;
// 광고 배너 본체

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "banner")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = true, length = 255)
    private String imageUrl;

    @Column(length = 255)
    private String linkUrl;

    @Column(name = "event_id")          // NULL 허용
    private Long eventId;

    @Column(name = "created_by")        // NULL 허용
    private Long createdBy;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;    // DB DEFAULT CURRENT_TIMESTAMP 사용

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_status_code_id", nullable = false)
    private BannerStatusCode bannerStatusCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_type_id", nullable = false)
    private BannerType bannerType;

    @Column(name = "booking_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal bookingRate;  // 0.00 ~ 100.00


    public void updateStatus(BannerStatusCode newStatus) {
        this.bannerStatusCode = newStatus;
    }

    public void updatePriority(Integer newPriority) {
        this.priority = newPriority;
    }

    public void updateInfo(String title, String imageUrl, String linkUrl, LocalDateTime startDate, LocalDateTime endDate, Integer priority,BannerType bannerType) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
        this.bannerType = bannerType;
        this.eventId = eventId; // ★ 세팅

    }

    public Banner(String title, String imageUrl, String linkUrl,
                  Integer priority, LocalDateTime startDate, LocalDateTime endDate,
                  BannerStatusCode statusCode, BannerType bannerType) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bannerStatusCode = statusCode;
        this.bannerType = bannerType;
        this.eventId = eventId; // ★ 세팅

    }

}

