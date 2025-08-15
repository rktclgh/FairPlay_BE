package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.event.entity.EventApply;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventApplyResponseDto {
    
    private Long eventApplyId;
    private String statusCode;
    private String statusName;
    private String eventEmail;
    private String businessNumber;
    private String businessName;
    private LocalDate businessDate;
    private Boolean verified;
    private String managerName;
    private String email;
    private String contactNumber;
    private String titleKr;
    private String titleEng;
    private String fileUrl;
    private LocalDateTime applyAt;
    private String adminComment;
    private LocalDateTime statusUpdatedAt;
    
    // EventDetail 정보들
    private Long locationId;
    private String address;
    private String locationName;
    private String locationDetail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String mainCategoryName;
    private String subCategoryName;
    private String bannerUrl;
    private String thumbnailUrl;
    private LocalDateTime updatedAt;

    public static EventApplyResponseDto from(EventApply eventApply) {
        EventApplyResponseDto dto = new EventApplyResponseDto();
        dto.setEventApplyId(eventApply.getEventApplyId());
        dto.setStatusCode(eventApply.getStatusCode().getCode());
        dto.setStatusName(eventApply.getStatusCode().getName());
        dto.setEventEmail(eventApply.getEventEmail());
        dto.setBusinessNumber(eventApply.getBusinessNumber());
        dto.setBusinessName(eventApply.getBusinessName());
        dto.setBusinessDate(eventApply.getBusinessDate());
        dto.setVerified(eventApply.getVerified());
        dto.setManagerName(eventApply.getManagerName());
        dto.setEmail(eventApply.getEmail());
        dto.setContactNumber(eventApply.getContactNumber());
        dto.setTitleKr(eventApply.getTitleKr());
        dto.setTitleEng(eventApply.getTitleEng());
        dto.setFileUrl(eventApply.getFileUrl());
        dto.setApplyAt(eventApply.getApplyAt());
        dto.setAdminComment(eventApply.getAdminComment());
        dto.setStatusUpdatedAt(eventApply.getStatusUpdatedAt());
        
        // Location 정보
        if (eventApply.getLocation() != null) {
            dto.setLocationId(eventApply.getLocation().getLocationId());
            dto.setLocationName(eventApply.getLocation().getPlaceName());
        }
        dto.setLocationDetail(eventApply.getLocationDetail());
        
        // 날짜 정보
        dto.setStartDate(eventApply.getStartDate());
        dto.setEndDate(eventApply.getEndDate());
        
        // 카테고리 정보
        if (eventApply.getMainCategory() != null) {
            dto.setMainCategoryName(eventApply.getMainCategory().getGroupName());
        }
        if (eventApply.getSubCategory() != null) {
            dto.setSubCategoryName(eventApply.getSubCategory().getCategoryName());
        }
        
        // 이미지 URL
        dto.setBannerUrl(eventApply.getBannerUrl());
        dto.setThumbnailUrl(eventApply.getThumbnailUrl());

        return dto;
    }

}
