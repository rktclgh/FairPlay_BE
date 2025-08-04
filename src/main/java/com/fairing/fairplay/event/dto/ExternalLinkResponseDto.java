package com.fairing.fairplay.event.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalLinkResponseDto {
    private String url;
    private String displayText;
}
