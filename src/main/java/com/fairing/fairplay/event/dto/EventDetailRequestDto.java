package com.fairing.fairplay.event.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailRequestDto { // 행사 상세 등록 
    
    private String titleKr;
    private String titleEng;
    private String address;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeUrl;
    private String locationDetail;
    private String hostName;
    private String contactInfo;
    private String bio;
    private String content;
    private String policy;
    private String officialUrl;
    private Integer eventTime;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer mainCategoryId;
    private Integer subCategoryId;
    private List<ExternalLinkRequestDto> externalLinks;
    private Boolean reentryAllowed;
    private Boolean checkOutAllowed;

    private String thumbnailFileKey;
    private List<String> contentFileKeys;
    private List<String> policyFileKeys;
}
