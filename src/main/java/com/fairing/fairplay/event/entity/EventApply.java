package com.fairing.fairplay.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "event_apply")
public class EventApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_apply_id")
    private Long eventApplyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id", nullable = false)
    private ApplyStatusCode statusCode;

    @Column(name = "event_email", nullable = false, length = 100)
    private String eventEmail;

    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;

    @Column(name = "business_name", nullable = false, length = 50)
    private String businessName;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean verified = false;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "title_kr", nullable = false, length = 200)
    private String titleKr;

    @Column(name = "title_eng", nullable = false, length = 200)
    private String titleEng;

    @Column(name = "file_url", length = 512, nullable = true)
    private String fileUrl;

    @CreatedDate
    @Column(name = "apply_at", nullable = false, updatable = false)
    private LocalDateTime applyAt;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "location_detail", length = 255)
    private String locationDetail;

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

    @Column(name = "banner_url", length = 255)
    private String bannerUrl;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;


    public void updateStatus(ApplyStatusCode newStatus, String adminComment) {
        this.statusCode = newStatus;
        this.adminComment = adminComment;
        this.statusUpdatedAt = LocalDateTime.now();
    }
}