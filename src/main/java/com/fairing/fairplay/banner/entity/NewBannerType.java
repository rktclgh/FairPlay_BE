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
@Table(name = "new_banner_type")
public class NewBannerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false)
    private Integer basePrice = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 메소드들
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isAvailable() {
        return this.isActive;
    }

    // 가격 관련 메소드
    public int calculatePrice(int days) {
        return this.basePrice * days;
    }

    // 배너 타입별 특성 확인
    public boolean isHeroType() {
        return "HERO".equals(this.code);
    }

    public boolean isSearchTopType() {
        return "SEARCH_TOP".equals(this.code);
    }

    public boolean isHotPickType() {
        return "HOT_PICK".equals(this.code);
    }

    public boolean isNewType() {
        return "NEW".equals(this.code);
    }
}