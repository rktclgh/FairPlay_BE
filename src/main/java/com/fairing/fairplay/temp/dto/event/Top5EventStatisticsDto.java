package com.fairing.fairplay.temp.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Top5EventStatisticsDto {
    private String eventName;
    private Long cnt;

}
