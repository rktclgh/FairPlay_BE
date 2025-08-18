package com.fairing.fairplay.booth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoothExternalLinkDto {
    private String displayText;
    private String url;
}
