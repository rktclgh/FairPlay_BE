package com.fairing.fairplay.statistics.dto.reservation;


import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRateAnalysisDto {

    ReservationRateSummaryDto summary;
    List<ReservationRateBySessionDto> sessionList;
    List<ReservationRateByTicketTypeDto> ticketList;

}
