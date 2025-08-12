package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "banner_priority_price",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_priority",
                columnNames = {"banner_type_id","priority"}))
public class BannerPriorityPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_priority_price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_type_id", nullable = false)
    private BannerType bannerType;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "price", nullable = false)
    private Integer price;
}
