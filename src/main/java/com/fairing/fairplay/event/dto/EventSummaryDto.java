package com.fairing.fairplay.event.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSummaryDto {  // 메인페이지, 검색 등에서 표시될 행사 정보
    private Long id;                            // 행사 ID
    private String eventCode;                   // [행사 담당자 전용] 행사 고유 코드
    private Boolean hidden;                   // [행사 담당자 전용] 일반 사용자에 숨겨진 상태
    private String title;                       // 국문 행사명
    private String titleEng;
    private Integer minPrice;                   // 최소 가격
    private String mainCategory;          // 메인 카테고리
    private String location;                    // 장소명
    private BigDecimal latitude;                // 위도
    private BigDecimal longitude;               // 경도
    private LocalDate startDate;                // 행사 시작일
    private LocalDate endDate;                  // 행사 종료일
    private String thumbnailUrl;                // 썸네일 URL
    private String region;                  // 지역명
    private String statusCode;
    private List<FileDto> files;

    @Getter
    @Setter
    @Builder
    public static class FileDto {
        private Long id;
        private String fileUrl;
        private String originalFileName;
    }

    @QueryProjection
    public EventSummaryDto(Long id, String eventCode, Boolean hidden, String title,
                           Integer minPrice, String mainCategory, String location,
                           BigDecimal latitude, BigDecimal longitude,
                           LocalDate startDate, LocalDate endDate, String thumbnailUrl, String region, String statusCode, String titleEng) {
        this.id = id;
        this.eventCode = eventCode;
        this.hidden = hidden;
        this.title = title;
        this.minPrice = minPrice;
        this.mainCategory = mainCategory;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUrl = thumbnailUrl;
        this.region = region;
        this.statusCode = statusCode;
        this.titleEng = titleEng;
    }
}