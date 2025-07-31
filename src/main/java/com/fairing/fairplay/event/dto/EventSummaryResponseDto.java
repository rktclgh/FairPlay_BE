package com.fairing.fairplay.event.dto;

import java.util.List;

import org.springframework.data.domain.Pageable;

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
public class EventSummaryResponseDto {

    private String message;
    private List<EventSummaryDto> events;
    private Pageable pageable;
    private Integer totalElements;
    private Integer totalPages;
    
}
