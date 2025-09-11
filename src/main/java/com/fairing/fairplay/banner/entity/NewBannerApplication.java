package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "new_banner_application")
public class NewBannerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "banner_type_id", nullable = false)
    private Long bannerTypeId;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false)
    @Builder.Default
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "approved_by")
    private Long approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 비즈니스 메소드들
    public void approve(Long approverId) {
        this.applicationStatus = ApplicationStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.applicationStatus = ApplicationStatus.REJECTED;
        this.adminComment = reason;
    }

    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    // 상태 확인 메소드들
    public boolean isPending() {
        return this.applicationStatus == ApplicationStatus.PENDING;
    }

    public boolean isApproved() {
        return this.applicationStatus == ApplicationStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.applicationStatus == ApplicationStatus.REJECTED;
    }

    public boolean isPaid() {
        return this.paymentStatus == PaymentStatus.PAID;
    }

    public boolean canCancel() {
        return isPending() || (isApproved() && !isPaid());
    }

    public boolean canPay() {
        return isApproved() && !isPaid();
    }

    // 상태 표시용 메소드
    public String getStatusDisplay() {
        if (isPaid()) return "결제 완료";
        if (isRejected()) return "반려됨";
        if (isApproved()) return "결제 대기";
        return "승인 대기";
    }

    // Enum 정의
    public enum ApplicationStatus {
        PENDING("승인 대기"),
        APPROVED("승인됨"),
        REJECTED("반려됨"),
        CANCELLED("취소됨");

        private final String description;

        ApplicationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum PaymentStatus {
        PENDING("결제 대기"),
        PAID("결제 완료"),
        CANCELLED("결제 취소");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}