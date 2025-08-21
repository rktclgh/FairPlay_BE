package com.fairing.fairplay.temp.dto.event;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Top3EventCompareDto {
    List<EventCompareDto> top3Events;
    private Long userCount;
    private Long reservationCount;
    private BigDecimal totalRevenue;
}
