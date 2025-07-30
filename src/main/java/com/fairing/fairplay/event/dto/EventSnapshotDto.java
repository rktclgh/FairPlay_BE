package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSnapshotDto {

    // Fields from Event
    private String eventCode;
    private String titleKr;
    private String titleEng;
    private boolean hidden;
    private Long managerId;
    private Integer eventStatusCodeId;

    // Fields from EventDetail
    private Long locationId;
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
    private Integer regionCodeId;

}