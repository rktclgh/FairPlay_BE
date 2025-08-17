package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailModificationDto {

    private String titleKr;
    private String titleEng;
    private Long locationId;
    private String address;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeUrl;
    private String locationDetail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String hostName;    // 주최자명
    private String contactInfo;
    private String bio;
    private String content;
    private String policy;
    private String officialUrl;
    private Integer eventTime;
    private String thumbnailUrl;
    private String bannerUrl;
    private Integer mainCategoryId;
    private String mainCategoryName;
    private Integer subCategoryId;
    private String subCategoryName;
    private Integer regionCodeId;
    private String businessNumber;  // 사업자 등록 번호
    private Boolean verified;
    private String managerName;     // 담당자명 (해당 행사 담당 유저 실명)
    private String managerPhone;    // 담당자 연락처
    private String managerEmail;    // 담당자 이메일
    private List<ExternalLinkRequestDto> externalLinks; // 외부 링크 이름 및 URL
    private Boolean reentryAllowed; // 재입장 허용 여부
    private Boolean checkInAllowed; // 체크인 허용 여부
    private Boolean checkOutAllowed; // 퇴장 스캔 여부
    private String hostCompany;     // 주최/기획사
    private Boolean age;            // 관람등급 (true: 청소년불가, false: 전체이용가)

    private List<EventDetailRequestDto.FileUploadDto> tempFiles;
    private List<Long> deletedFileIds; // 삭제할 파일 ID 목록
}