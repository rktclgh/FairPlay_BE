package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.file.dto.TempFileUploadDto;
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
    private List<TempFileUploadDto> tempFiles;
    
    // EventDetail과 비슷한 정보들
    private Long locationId;
    private String locationDetail;
    
    // 새로운 장소 정보 (카카오맵에서 받은 데이터)
    private String address;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeUrl;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer mainCategoryId;
    private Integer subCategoryId;
    
    // 이미지 파일들은 tempFiles에서 처리
    // 기존 bannerUrl, thumbnailUrl은 하위 호환성을 위해 유지
    private String bannerUrl;
    private String thumbnailUrl;

}
