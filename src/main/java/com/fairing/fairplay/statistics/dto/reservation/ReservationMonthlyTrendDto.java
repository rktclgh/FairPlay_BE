package com.fairing.fairplay.statistics.dto.reservation;

import java.time.YearMonth;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationMonthlyTrendDto {
    private YearMonth yearMonth;
    private Long reservations;
}
