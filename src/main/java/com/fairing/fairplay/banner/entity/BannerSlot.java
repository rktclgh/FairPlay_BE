package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "banner_slot",
        uniqueConstraints = @UniqueConstraint(name = "uk_slot",
                columnNames = {"banner_type_id","slot_date","priority"}))
public class BannerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_type_id", nullable = false)
    private BannerType bannerType;        // HERO / SEARCH_TOP 등

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, columnDefinition = "ENUM('HERO','SEARCH_TOP')")
    private BannerSlotType slotType;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;           // 하루 단위

    @Column(name = "priority", nullable = false)
    private Integer priority;             // 1..N

    @Column(name = "quota", nullable = false)
    private Integer quota;                // 현재는 1

    @Column(name = "price", nullable = false)
    private Integer price;                // 생성 시점 가격 스냅샷

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('AVAILABLE','LOCKED','SOLD')")
    private BannerSlotStatus status;

    @Column(name = "locked_by")
    private Long lockedBy;                // users.user_id

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_banner_id")
    private Banner soldBanner;            // 결제 후 생성된 배너

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = BannerSlotStatus.AVAILABLE;
        if (slotType == null) slotType = BannerSlotType.HERO;
        if (quota == null) quota = 1;
    }
}
