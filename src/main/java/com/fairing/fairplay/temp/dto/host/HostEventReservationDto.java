package com.fairing.fairplay.temp.dto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HostEventReservationDto {
    Double totalRate;
    Double averageRate;
    Double topRate;
    Double bottomRate;

}
