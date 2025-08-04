package com.fairing.fairplay.event.dto;

import lombok.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSummaryResponseDto {  // 행사 목록

    private String message;
    private List<EventSummaryDto> events;
    private Pageable pageable;
    private Long totalElements;
    private Integer totalPages;
    
}
