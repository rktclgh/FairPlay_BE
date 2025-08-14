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
public class EventApplyRequestDto {
    
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
    
    // 파일 업로드 정보
    private List<FileUploadDto> tempFiles;
    
    // EventDetail과 비슷한 정보들
    private Long locationId;
    private String locationDetail;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer mainCategoryId;
    private Integer subCategoryId;
    
    // 이미지 파일들은 tempFiles에서 처리
    // 기존 bannerUrl, thumbnailUrl은 하위 호환성을 위해 유지
    private String bannerUrl;
    private String thumbnailUrl;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUploadDto {
        private String s3Key;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private String usage; // "application_file", "banner", "thumbnail" 등
    }
}
