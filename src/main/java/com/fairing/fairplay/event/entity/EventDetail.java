package com.fairing.fairplay.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_detail")
public class EventDetail {

    @Id
    private Long eventDetailId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "event_detail_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "location_detail", length = 255)
    private String locationDetail;

    @Column(name = "host_name", length = 255)
    private String hostName;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    @Column(length = 255)
    private String bio;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String policy;

    @Column(name = "official_url", length = 255)
    private String officialUrl;

    @Column
    private Integer eventTime;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_category", referencedColumnName = "group_id", nullable = false)
    private MainCategory mainCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category", referencedColumnName = "category_id", nullable = false)
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code_id", nullable = false)
    private RegionCode regionCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reentry_allowed", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean reentryAllowed = true;

    @Column(name = "check_out_allowed", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean checkOutAllowed = false;
}