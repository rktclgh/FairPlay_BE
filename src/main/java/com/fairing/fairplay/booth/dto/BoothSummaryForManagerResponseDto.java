package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.Booth;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BoothSummaryForManagerResponseDto {
    private Long boothId;
    private String boothTitle;
    private String boothTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private Boolean isDeleted;

    public static BoothSummaryForManagerResponseDto from (Booth booth) {
        BoothSummaryForManagerResponseDto dto = new BoothSummaryForManagerResponseDto();
        dto.setBoothId(booth.getId());
        dto.setBoothTitle(booth.getBoothTitle());
        dto.setBoothTypeName(booth.getBoothType().getName());
        dto.setStartDate(booth.getStartDate());
        dto.setEndDate(booth.getEndDate());
        dto.setLocation(booth.getLocation());
        dto.setIsDeleted(booth.getIsDeleted());
        return dto;
    }
}
