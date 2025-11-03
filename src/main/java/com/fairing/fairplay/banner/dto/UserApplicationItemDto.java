package com.fairing.fairplay.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserApplicationItemDto {
    private LocalDate date;
    private Integer priority;
    private Integer price;
}
