package com.fairing.fairplay.banner.entity;

import com.fairing.fairplay.event.entity.ApplyStatusCode;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "banner_application")
public class BannerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Users applicantId;               // users.user_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_type_id", nullable = false)
    private BannerType bannerType;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "requested_priority", nullable = false)
    private Integer requestedPriority;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id", nullable = false)
    private ApplyStatusCode statusCode;     // PENDING/APPROVED/REJECTED

    @Lob
    @Column(name = "admin_comment")
    private String adminComment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_by")
    private Long approvedBy;                // users.user_id

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private BannerPaymentStatus paymentStatus = BannerPaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "bannerApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BannerApplicationSlot> slots = new ArrayList<>();

    public void addSlot(BannerApplicationSlot slot) {
           if (slot == null) return;
           slot.setBannerApplication(this);
           this.slots.add(slot);
        }

        public void removeSlot(BannerApplicationSlot slot) {
            if (slot == null) return;
            this.slots.remove(slot);
            slot.setBannerApplication(null);
        }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void updatePaymentStatus(BannerPaymentStatus newStatus) {
        this.paymentStatus = newStatus;
        if (newStatus == BannerPaymentStatus.PAID) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
