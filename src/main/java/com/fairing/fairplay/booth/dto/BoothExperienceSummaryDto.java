package com.fairing.fairplay.booth.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceSummaryDto {

    private Long experienceId; // 체험 ID
    
    private String experienceTitle; // 체험 제목
    
    private String boothName; // 부스명
    
    private Integer maxCapacity; // 최대 정원
    
    private Integer currentParticipants; // 현재 체험중 인원 수
    
    private Integer waitingCount; // 대기 인원 수
    
    private List<String> currentParticipantNames; // 현재 체험중인 인원 이름 목록
    
    private String nextParticipantName; // 다음 입장 예약자명
    
    private Double congestionRate; // 혼잡도 (0-100)
    
    private Boolean isReservationAvailable; // 예약 가능 여부
}