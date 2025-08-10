package com.fairing.fairplay.booth.entity;

import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booth_experience_reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId; // 예약 ID (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id", nullable = false)
    private BoothExperience boothExperience; // 체험 정보 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // 예약자 정보 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id", nullable = false)
    private BoothExperienceStatusCode experienceStatusCode; // 체험 진행 상태 (FK)

    @Column(name = "queue_position")
    private Integer queuePosition; // 대기 순번 (1부터 시작)

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt; // 예약일시 (대기 순서 기준)

    @Column(name = "ready_at")
    private LocalDateTime readyAt; // 입장 가능 알림 시간

    @Column(name = "started_at")
    private LocalDateTime startedAt; // 체험 시작 시간

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 체험 완료 시간

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt; // 취소 시간

    @Column(name = "notes", length = 500)
    private String notes; // 특이사항 또는 메모

    @PrePersist
    protected void onCreate() {
        if (reservedAt == null) {
            reservedAt = LocalDateTime.now();
        }
    }

    // 대기 시간 계산 (분 단위)
    public Long getWaitingMinutes() {
        if (reservedAt == null) return 0L;
        
        LocalDateTime endTime = switch (experienceStatusCode.getCode()) {
            case "READY" -> readyAt != null ? readyAt : LocalDateTime.now();
            case "IN_PROGRESS" -> startedAt != null ? startedAt : LocalDateTime.now();
            case "COMPLETED" -> completedAt != null ? completedAt : LocalDateTime.now();
            case "CANCELLED" -> cancelledAt != null ? cancelledAt : LocalDateTime.now();
            default -> LocalDateTime.now();
        };
        
        return java.time.Duration.between(reservedAt, endTime).toMinutes();
    }

    // 체험 소요 시간 계산 (분 단위)
    public Long getExperienceDurationMinutes() {
        if (startedAt == null || completedAt == null) return 0L;
        return java.time.Duration.between(startedAt, completedAt).toMinutes();
    }

    // 현재 상태가 활성 상태인지 확인 (취소되지 않은 상태)
    public Boolean isActive() {
        return !"CANCELLED".equals(experienceStatusCode.getCode());
    }
}