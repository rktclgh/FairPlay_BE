package com.fairing.fairplay.booth.entity;

import com.fairing.fairplay.event.entity.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BoothApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "booth_email", nullable = false, length = 100)
    private String boothEmail;

    @Column(name = "booth_description", columnDefinition = "TEXT")
    private String boothDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_type_id", nullable = false)
    private BoothType boothType;

    @Column(name = "manager_name", nullable = false, length = 20)
    private String managerName;

    @Column(name = "email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "apply_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime applyAt;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_application_status_code_id", nullable = false)
    private BoothApplicationStatusCode boothApplicationStatusCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_payment_status_code_id", nullable = false)
    private BoothPaymentStatusCode boothPaymentStatusCode;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "booth_title", nullable = false, length = 100)
    private String boothTitle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "booth_banner_url", length = 512)
    private String boothBannerUrl;

    @OneToMany(mappedBy = "boothApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BoothExternalLink> boothExternalLinks = new HashSet<>();

    public void updateStatus(BoothApplicationStatusCode newStatus, String adminComment) {
        this.boothApplicationStatusCode = newStatus;
        this.adminComment = adminComment;
        this.statusUpdatedAt = LocalDateTime.now();
    }
}
