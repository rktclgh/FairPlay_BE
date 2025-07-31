package com.fairing.fairplay.event.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fairing.fairplay.event.entity.RegionCode;
import com.fairing.fairplay.event.entity.MainCategory;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSummaryDto {  // 메인페이지, 검색 등에서 표시될 행사 정보
    private Long id;                            // 행사 ID
    private String eventCode;                   // [행사 담당자 전용] 행사 고유 코드
    private Boolean isHidden;                   // [행사 담당자 전용] 일반 사용자에 숨겨진 상태
    private String title;                       // 국문 행사명
    private Integer minPrice;                   // 최소 가격
    private MainCategory mainCategory;          // 메인 카테고리
    private String location;                    // 건물명
    private LocalDate startDate;                // 행사 시작일
    private LocalDate endDate;                  // 행사 종료일
    private String thumbnailUrl;                // 썸네일 URL
    private RegionCode region;                  // 지역 코드
}
