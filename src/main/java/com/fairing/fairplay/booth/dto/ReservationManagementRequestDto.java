package com.fairing.fairplay.booth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationManagementRequestDto {

    private Long boothId; // 부스 ID 필터
    
    private String reserverName; // 예약자 이름 검색
    
    private String reserverPhone; // 예약자 전화번호 검색
    
    private String experienceDate; // 체험일 (YYYY-MM-DD)
    
    private String statusCode; // 체험 상태 필터 (WAITING, READY, IN_PROGRESS, COMPLETED, CANCELLED)
    
    private int page = 0; // 페이지 번호 (0부터 시작)
    
    private int size = 20; // 페이지 크기
    
    private String sortBy = "reservedAt"; // 정렬 기준 (reservedAt, queuePosition 등)
    
    private String sortDirection = "asc"; // 정렬 방향 (asc, desc)
}