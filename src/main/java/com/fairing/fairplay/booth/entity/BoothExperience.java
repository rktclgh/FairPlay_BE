package com.fairing.fairplay.booth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "booth_experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "experience_id")
    private Long experienceId; // 체험 ID (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth; // 부스 정보 (FK)

    @Column(name = "title", nullable = false, length = 100)
    private String title; // 체험 제목

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 체험 설명

    @Column(name = "experience_date", nullable = false)
    private LocalDate experienceDate; // 체험 날짜

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 운영 시작 시간 (예: 09:00)

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // 운영 종료 시간 (예: 18:00)

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // 체험 소요 시간 (분 단위, 예: 20분)

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity; // 최대 동시 체험 인원

    @Builder.Default
    @Column(name = "allow_waiting")
    private Boolean allowWaiting = true; // 대기열 허용 여부

    @Column(name = "max_waiting_count")
    private Integer maxWaitingCount; // 최대 대기 인원 (null이면 무제한)

    @Builder.Default
    @Column(name = "allow_duplicate_reservation")
    private Boolean allowDuplicateReservation = false; // 사용자 중복 예약 허용 여부

    @Builder.Default
    @Column(name = "is_reservation_enabled")
    private Boolean isReservationEnabled = true; // 예약 가능 여부

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 등록일시

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일시

    @OneToMany(mappedBy = "boothExperience", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoothExperienceReservation> reservations; // 예약 목록

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 현재 체험중 인원 수 (계산 필드)
    public Integer getCurrentParticipants() {
        if (reservations == null) return 0;
        return (int) reservations.stream()
                .filter(r -> "IN_PROGRESS".equals(r.getExperienceStatusCode().getCode()))
                .count();
    }

    // 현재 대기 인원 수 (계산 필드)
    public Integer getWaitingCount() {
        if (reservations == null) return 0;
        return (int) reservations.stream()
                .filter(r -> "WAITING".equals(r.getExperienceStatusCode().getCode()) || 
                           "READY".equals(r.getExperienceStatusCode().getCode()))
                .count();
    }

    // 혼잡도 계산 (0-100)
    public Double getCongestionRate() {
        if (maxCapacity == 0) return 0.0;
        return (getCurrentParticipants().doubleValue() / maxCapacity) * 100.0;
    }

    // 예약 가능 여부 확인
    public Boolean isReservationAvailable() {
        if (!isReservationEnabled) return false;
        if (!allowWaiting) {
            return getCurrentParticipants() < maxCapacity;
        }
        if (maxWaitingCount != null) {
            return getWaitingCount() < maxWaitingCount;
        }
        return true; // 대기열 허용하고 최대 대기 수 제한 없으면 항상 가능
    }

    // 체험 정보 업데이트 메서드
    public void updateExperience(String title, String description, LocalDate experienceDate,
                               LocalTime startTime, LocalTime endTime, Integer durationMinutes,
                               Integer maxCapacity, Boolean allowWaiting, Integer maxWaitingCount,
                               Boolean allowDuplicateReservation, Boolean isReservationEnabled) {
        this.title = title;
        this.description = description;
        this.experienceDate = experienceDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.maxCapacity = maxCapacity;
        this.allowWaiting = allowWaiting;
        this.maxWaitingCount = maxWaitingCount;
        this.allowDuplicateReservation = allowDuplicateReservation;
        this.isReservationEnabled = isReservationEnabled;
    }
}