package com.fairing.fairplay.statistics.dto.reservation;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReservationStatsByCategoryDto {

    private Integer totalReservation;
    private String mainCategory;
}
