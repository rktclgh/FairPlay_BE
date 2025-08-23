package com.fairing.fairplay.temp.dto.host;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DailyTrendDto {
    private LocalDate date;
    private Double reservationRate;

}
