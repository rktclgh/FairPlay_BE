package com.fairing.fairplay.banner.dto;

import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationSlotDto {
    LocalDate slotDate;
    Integer priority;
    Integer price;
}


