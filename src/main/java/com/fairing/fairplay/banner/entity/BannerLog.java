package com.fairing.fairplay.banner.entity;
// 배너 변경 이력 로그
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fairing.fairplay.admin.entity.AdminAccount;

import java.time.LocalDateTime;

@Entity
@Table(name = "banner_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_log_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", nullable = false)
    private Banner banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private AdminAccount changedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_action_code_id", nullable = false)
    private BannerActionCode actionCode;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }

    @Builder
    public BannerLog(Banner banner, AdminAccount changedBy, BannerActionCode actionCode) {
        this.banner = banner;
        this.changedBy = changedBy;
        this.actionCode = actionCode;
    }
}
