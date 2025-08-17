package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BoothExternalLinkDto {
    private String displayText;
    private String url;
}
