package com.fairing.fairplay.booth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceReservationRequestDto {

    private String notes; // 특이사항 또는 메모 (선택사항)
}