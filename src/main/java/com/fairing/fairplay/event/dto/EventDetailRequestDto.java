package com.fairing.fairplay.event.dto;


import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailRequestDto { // 행사 상세 등록 
    
    private String titleKr;
    private String titleEng;
    private String address;
    private String buildingName;
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

}
