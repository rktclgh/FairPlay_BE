package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.BoothExperience;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceResponseDto {

    private Long experienceId; // 체험 ID

    private Long boothId; // 부스 ID

    private String boothName; // 부스명

    private Long eventId; // 행사 ID

    private String eventName; // 행사명

    private String title; // 체험 제목

    private String description; // 체험 설명

    private LocalDate experienceDate; // 체험 날짜

    private LocalTime startTime; // 운영 시작 시간

    private LocalTime endTime; // 운영 종료 시간

    private Integer durationMinutes; // 체험 소요 시간

    private Integer maxCapacity; // 최대 동시 체험 인원

    private Integer currentParticipants; // 현재 체험중 인원

    private Integer waitingCount; // 현재 대기 인원

    private Double congestionRate; // 혼잡도 (%)

    private Boolean allowWaiting; // 대기열 허용 여부

    private Integer maxWaitingCount; // 최대 대기 인원

    private Boolean allowDuplicateReservation; // 중복 예약 허용 여부

    private Boolean isReservationEnabled; // 예약 가능 여부

    private Boolean isReservationAvailable; // 현재 예약 가능 상태

    private LocalDateTime createdAt; // 등록일시

    private LocalDateTime updatedAt; // 수정일시

    // 엔티티에서 DTO로 변환
    public static BoothExperienceResponseDto fromEntity(BoothExperience entity) {
        return BoothExperienceResponseDto.builder()
                .experienceId(entity.getExperienceId())
                .boothId(entity.getBooth().getId())
                .boothName(entity.getBooth().getBoothTitle())
                .eventId(entity.getBooth().getEvent().getEventId())
                .eventName(entity.getBooth().getEvent().getTitleKr())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .experienceDate(entity.getExperienceDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .durationMinutes(entity.getDurationMinutes())
                .maxCapacity(entity.getMaxCapacity())
                .currentParticipants(entity.getCurrentParticipants())
                .waitingCount(entity.getWaitingCount())
                .congestionRate(entity.getCongestionRate())
                .allowWaiting(entity.getAllowWaiting())
                .maxWaitingCount(entity.getMaxWaitingCount())
                .allowDuplicateReservation(entity.getAllowDuplicateReservation())
                .isReservationEnabled(entity.getIsReservationEnabled())
                .isReservationAvailable(entity.isReservationAvailable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}