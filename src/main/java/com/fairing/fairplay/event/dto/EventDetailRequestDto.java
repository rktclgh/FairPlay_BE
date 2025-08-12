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
    private Boolean checkInAllowed;
    private Boolean checkOutAllowed;

    private List<FileUploadDto> tempFiles; // 임시 업로드 파일 정보
    private List<Long> deletedFileIds; // 삭제할 파일 ID 목록

    @Getter
    @Setter
    public static class FileUploadDto {
        private String s3Key; // 임시 파일 키
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private String usage; // 파일 용도 (e.g., "thumbnail", "content", "bio")
    }
}
