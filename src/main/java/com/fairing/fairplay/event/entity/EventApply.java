package com.fairing.fairplay.event.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "event_apply")
public class EventApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_apply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id", nullable = false)
    private ApplyStatusCode statusCode;

    @Column(name = "event_email", nullable = false, length = 100)
    private String eventEmail;

    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;

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

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    @Column(name = "apply_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime applyAt;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;
}
