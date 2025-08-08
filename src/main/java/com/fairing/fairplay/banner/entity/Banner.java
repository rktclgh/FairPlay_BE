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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Column(length = 255)
    private String linkUrl;

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

    @Column(nullable = false)
    private boolean hot = false;

    @Column(name="md_pick", nullable = false)
    private boolean mdPick = false;

    public void updateStatus(BannerStatusCode newStatus) {
        this.bannerStatusCode = newStatus;
    }

    public void updatePriority(Integer newPriority) {
        this.priority = newPriority;
    }

    public void updateInfo(String title, String imageUrl, String linkUrl, LocalDateTime startDate, LocalDateTime endDate, Integer priority,BannerType bannerType, boolean hot, boolean mdPick) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
        this.bannerType = bannerType;
        this.hot = hot;
        this.mdPick = mdPick;
    }

    public Banner(String title, String imageUrl, String linkUrl,
                  Integer priority, LocalDateTime startDate, LocalDateTime endDate,
                  BannerStatusCode statusCode, BannerType bannerType, boolean hot, boolean mdPick) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bannerStatusCode = statusCode;
        this.bannerType = bannerType;
        this.hot = hot;
        this.mdPick = mdPick;
    }

}

