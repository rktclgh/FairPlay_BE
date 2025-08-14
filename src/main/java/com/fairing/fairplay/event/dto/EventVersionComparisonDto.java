package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventVersionComparisonDto {
    
    private Long eventId;
    private Integer version1;
    private Integer version2;
    private EventSnapshotDto snapshot1;
    private EventSnapshotDto snapshot2;
    private Object fieldDifferences;               // JSON 형태의 필드별 차이점
}