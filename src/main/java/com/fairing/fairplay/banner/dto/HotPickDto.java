package com.fairing.fairplay.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class HotPickDto {
    private Long id;         // = eventId
    private String title;    // 이벤트 제목
    private String date;     // "YYYY-MM-DD ~ YYYY-MM-DD" or 단일일자
    private String location; // 장소
    private String category; // 메인 카테고리 표시명(예: 공연/박람회 등)
    private String image;  // 배너 이미지(없으면 이벤트 썸네일)
}
