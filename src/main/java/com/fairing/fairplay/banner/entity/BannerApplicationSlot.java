package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "banner_application_slot",
        uniqueConstraints = @UniqueConstraint(name = "uq_app_slot", columnNames = {"slot_id"}))
public class BannerApplicationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_slot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_application_id", nullable = false)
    private BannerApplication bannerApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private BannerSlot slot;

    @Column(name = "item_price", nullable = false)
    private Integer itemPrice;
}
