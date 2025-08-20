package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.Booth;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothSummaryResponseDto {
    private Long boothId;
    private String boothTitle;
    private String boothBannerUrl;
    private String location;

    public static BoothSummaryResponseDto from (Booth booth) {
        BoothSummaryResponseDto dto = new BoothSummaryResponseDto();
        dto.setBoothId(booth.getId());
        dto.setBoothTitle(booth.getBoothTitle());
        dto.setBoothBannerUrl(booth.getBoothBannerUrl());
        dto.setLocation(booth.getLocation());

        return dto;
    }
}
