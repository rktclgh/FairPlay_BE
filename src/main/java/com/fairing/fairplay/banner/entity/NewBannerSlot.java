package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "new_banner_slot")
public class NewBannerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "banner_application_id", nullable = false)
    private Long bannerApplicationId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.RESERVED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // 비즈니스 메소드들
    public void activate() {
        this.status = Status.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = Status.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    // 상태 확인 메소드들
    public boolean isReserved() {
        return this.status == Status.RESERVED;
    }

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    public boolean isExpired() {
        return this.status == Status.EXPIRED;
    }

    // 오늘 날짜 기준 활성 여부 확인
    public boolean isTodayActive() {
        return isActive() && LocalDate.now().equals(this.slotDate);
    }

    // 미래 슬롯 여부 확인
    public boolean isFutureSlot() {
        return this.slotDate.isAfter(LocalDate.now());
    }

    // Enum 정의
    public enum Status {
        RESERVED("예약됨"),
        ACTIVE("활성"),
        EXPIRED("만료됨");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}