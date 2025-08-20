package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.BoothExternalLink; // Import the entity
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoothExternalLinkDto {
    private String displayText;
    private String url;

    public static BoothExternalLinkDto from(BoothExternalLink boothExternalLink) {
        return BoothExternalLinkDto.builder()
                .displayText(boothExternalLink.getDisplayText())
                .url(boothExternalLink.getUrl())
                .build();
    }
}
