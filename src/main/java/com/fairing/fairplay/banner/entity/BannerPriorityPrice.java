package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banner_type_id", nullable = false)
    @NotNull
    private BannerType bannerType;

    @Column(name = "priority", nullable = false)
    @NotNull
    @Positive
    private Integer priority;

    @Column(name = "price", nullable = false)
    @NotNull
    @Positive
    private Integer price;
}
