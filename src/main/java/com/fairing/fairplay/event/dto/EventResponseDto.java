package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto { // 행사 등록 후 응답

    private String message;
    private Long eventId;
    private Long managerId;
    private String eventCode;   // 슬러그 + Hashid -> e.g. EVT-BzD4X7e9
    private Integer version;
    
}
