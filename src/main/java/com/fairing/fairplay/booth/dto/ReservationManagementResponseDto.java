package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationManagementResponseDto {

    private Long reservationId; // 예약 ID
    
    private String boothName; // 부스명
    
    private String experienceTitle; // 체험명
    
    private String experienceDate; // 체험일 (YYYY-MM-DD)
    
    private String reserverName; // 예약자명
    
    private String reserverPhone; // 예약자 전화번호
    
    private Boolean canEnter; // 입장 가능 여부 (가능/불가)
    
    private Integer queuePosition; // 대기 순서
    
    private String statusCode; // 체험 상태 코드
    
    private String statusName; // 체험 상태명 (대기중, 입장가능, 체험중, 완료)
    
    private LocalDateTime reservedAt; // 예약 일시
    
    private LocalDateTime readyAt; // 입장 가능 시간
    
    private LocalDateTime startedAt; // 체험 시작 시간
    
    private LocalDateTime completedAt; // 체험 완료 시간
    
    private String notes; // 특이사항

    // 엔티티에서 DTO로 변환
    public static ReservationManagementResponseDto fromEntity(BoothExperienceReservation entity) {
        return ReservationManagementResponseDto.builder()
                .reservationId(entity.getReservationId())
                .boothName(entity.getBoothExperience().getBooth().getBoothTitle())
                .experienceTitle(entity.getBoothExperience().getTitle())
                .experienceDate(entity.getBoothExperience().getExperienceDate().toString())
                .reserverName(entity.getUser().getName())
                .reserverPhone(entity.getUser().getPhone())
                .canEnter(determineCanEnter(entity))
                .queuePosition(entity.getQueuePosition())
                .statusCode(entity.getExperienceStatusCode().getCode())
                .statusName(entity.getExperienceStatusCode().getName())
                .reservedAt(entity.getReservedAt())
                .readyAt(entity.getReadyAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .notes(entity.getNotes())
                .build();
    }

    // 입장 가능 여부 판단 로직
    private static Boolean determineCanEnter(BoothExperienceReservation entity) {
        String statusCode = entity.getExperienceStatusCode().getCode();
        
        // READY 상태이거나, 현재 체험중인 인원이 최대 정원보다 적은 경우 입장 가능
        if ("READY".equals(statusCode)) {
            return true;
        }
        
        if ("WAITING".equals(statusCode)) {
            // 현재 체험중인 인원이 최대 정원보다 적은지 확인
            int currentParticipants = entity.getBoothExperience().getCurrentParticipants();
            int maxCapacity = entity.getBoothExperience().getMaxCapacity();
            return currentParticipants < maxCapacity;
        }
        
        return false;
    }
}