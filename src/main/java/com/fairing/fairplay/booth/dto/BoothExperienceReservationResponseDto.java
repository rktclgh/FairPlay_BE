package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceReservationResponseDto {

    private Long reservationId; // 예약 ID

    private Long experienceId; // 체험 ID

    private String experienceTitle; // 체험 제목

    private Long boothId; // 부스 ID

    private String boothName; // 부스명

    private Long eventId; // 행사 ID

    private String eventName; // 행사명

    private Long userId; // 예약자 ID

    private String userName; // 예약자 이름

    private String statusCode; // 상태 코드

    private String statusName; // 상태명

    private Integer queuePosition; // 대기 순번

    private LocalDateTime reservedAt; // 예약일시

    private LocalDateTime readyAt; // 입장 가능 시간

    private LocalDateTime startedAt; // 체험 시작 시간

    private LocalDateTime completedAt; // 체험 완료 시간

    private LocalDateTime cancelledAt; // 취소 시간

    private Long waitingMinutes; // 대기 시간 (분)

    private Long experienceDurationMinutes; // 체험 소요 시간 (분)

    private String notes; // 특이사항 또는 메모

    private Boolean isActive; // 활성 상태 여부

    // 엔티티에서 DTO로 변환
    public static BoothExperienceReservationResponseDto fromEntity(BoothExperienceReservation entity) {
        return BoothExperienceReservationResponseDto.builder()
                .reservationId(entity.getReservationId())
                .experienceId(entity.getBoothExperience().getExperienceId())
                .experienceTitle(entity.getBoothExperience().getTitle())
                .boothId(entity.getBoothExperience().getBooth().getId())
                .boothName(entity.getBoothExperience().getBooth().getBoothTitle())
                .eventId(entity.getBoothExperience().getBooth().getEvent().getEventId())
                .eventName(entity.getBoothExperience().getBooth().getEvent().getTitleKr())
                .userId(entity.getUser().getUserId())
                .userName(entity.getUser().getName())
                .statusCode(entity.getExperienceStatusCode().getCode())
                .statusName(entity.getExperienceStatusCode().getName())
                .queuePosition(entity.getQueuePosition())
                .reservedAt(entity.getReservedAt())
                .readyAt(entity.getReadyAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .waitingMinutes(entity.getWaitingMinutes())
                .experienceDurationMinutes(entity.getExperienceDurationMinutes())
                .notes(entity.getNotes())
                .isActive(entity.isActive())
                .build();
    }
}