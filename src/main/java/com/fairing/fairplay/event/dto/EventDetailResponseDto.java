package com.fairing.fairplay.event.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailResponseDto {

    private String message;

    /* 관리자 전용 */
    private Long managerId;     // 행사 관리자 ID
    private String eventCode;   // 행사 고유 코드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    private Integer viewCount;

    /******************/

    /* 제목 */
    private String titleKr;
    private String titleEng;

    /* 행사 상태 */
    private Boolean hidden;    // 일반 사용자에 행사 공개 여부
    private String eventStatusCode;    // UPCOMING / ONGOING / ENDED

    /* 카테고리 */
    private String mainCategory;
    private String subCategory;

    /* 위치 정보 */
    private String address;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeUrl;
    private String locationDetail;
    private String region;

    /* 행사 시작/종료일 */
    private LocalDate startDate;
    private LocalDate endDate;

    /* 썸네일 */
    private String thumbnailUrl;

    /* 주최처/문의처 */
    private String hostName;
    private String contactInfo;
    private String officialUrl;

    /* 담당자 정보 */
    private String managerName;
    private String managerPhone;
    private String managerEmail;
    private String managerBusinessNumber;

    /* 행사 정보 */
    private String bio;
    private String content;
    private String policy;
    private Integer eventTime;

    /* 외부 링크 */
    private List<ExternalLinkResponseDto> externalLinks;

    /* 재입장 허용 여부 */
    private Boolean reentryAllowed;

    /* 퇴장 스캔 여부 */
    private Boolean checkOutAllowed;

    /* 주최/기획사 */
    private String hostCompany;

    /* 관람등급 (true: 청소년불가, false: 전체이용가) */
    private Boolean age;
}
