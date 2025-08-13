package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.ticket.dto.TicketSnapshotDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSnapshotDto {

    // from Event
    private String eventCode;
    private String titleKr;
    private String titleEng;
    private boolean hidden;
    private Long managerId;
    private Integer eventStatusCodeId;
    private List<TicketSnapshotDto> tickets; 

    // from EventDetail
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
    private List<EventSnapshotDto.ExternalLinkSnapshot> externalLinks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalLinkSnapshot {
        private String url;
        private String displayText;
    }

}
