package com.fairing.fairplay.banner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "new_banner_log")
public class NewBannerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "banner_application_id", nullable = false)
    private Long bannerApplicationId;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 편의 메소드들
    public boolean isStatusChange() {
        return this.oldStatus != null && this.newStatus != null && !this.oldStatus.equals(this.newStatus);
    }

    public boolean isAdminAction() {
        return this.adminId != null;
    }

    public boolean isSystemAction() {
        return this.adminId == null;
    }

    // 액션 타입별 확인 메소드
    public boolean isCreatedAction() {
        return "CREATED".equals(this.actionType);
    }

    public boolean isApprovedAction() {
        return "APPROVED".equals(this.actionType);
    }

    public boolean isRejectedAction() {
        return "REJECTED".equals(this.actionType);
    }

    public boolean isPaidAction() {
        return "PAYMENT_COMPLETED".equals(this.actionType);
    }

    public boolean isDeletedAction() {
        return "HARD_DELETED".equals(this.actionType);
    }

    // 로그 메시지 생성
    public String getDisplayMessage() {
        StringBuilder message = new StringBuilder();
        
        switch (this.actionType) {
            case "CREATED":
                message.append("배너 신청서가 생성되었습니다.");
                break;
            case "APPROVED":
                message.append("배너 신청서가 승인되었습니다.");
                break;
            case "REJECTED":
                message.append("배너 신청서가 반려되었습니다.");
                break;
            case "PAYMENT_COMPLETED":
                message.append("결제가 완료되어 배너가 활성화되었습니다.");
                break;
            case "HARD_DELETED":
                message.append("관리자에 의해 완전 삭제되었습니다.");
                break;
            default:
                message.append(this.actionType);
        }
        
        if (this.comment != null && !this.comment.trim().isEmpty()) {
            message.append(" (").append(this.comment).append(")");
        }
        
        return message.toString();
    }

    // 상태 변경 정보
    public String getStatusChangeInfo() {
        if (!isStatusChange()) {
            return "";
        }
        return String.format("%s → %s", this.oldStatus, this.newStatus);
    }
}