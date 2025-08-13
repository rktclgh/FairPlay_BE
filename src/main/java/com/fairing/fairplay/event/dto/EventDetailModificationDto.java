package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailModificationDto {

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
    private String bannerUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer mainCategoryId;
    private Integer subCategoryId;
    private Integer regionCodeId;
    private Boolean reentryAllowed;
    private Boolean checkOutAllowed;
    private String hostCompany;
    private Boolean age;

    private List<EventDetailRequestDto.FileUploadDto> tempFiles;
}