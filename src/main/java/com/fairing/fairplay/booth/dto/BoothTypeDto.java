package com.fairing.fairplay.booth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothTypeDto {
    private Long id;
    private String name;
    private String size;
    private Integer price;
    private Integer maxApplicants;
    private Integer currentApplicants;
    private Boolean available;
}
