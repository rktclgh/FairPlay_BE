package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.service.event.EventComparisonService;
import com.fairing.fairplay.statistics.dto.event.EventComparisonPageResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventComparisonResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/event-comparison")
@RequiredArgsConstructor
public class EventComparisonController {

    private final EventComparisonService eventComparisonService;

    /**
     * 행사별 비교 페이지 전체 데이터 조회
     */
    @GetMapping("/page-data")
    public EventComparisonPageResponseDto getPageData() {
        return eventComparisonService.getComparisonPageData();
    }

    /**
     * 상태별 필터링
     */
    @GetMapping("/filter/status")
    public List<EventComparisonResponseDto> getEventsByStatus(
            @RequestParam
            @jakarta.validation.constraints.Pattern(regexp = "ONGOING|UPCOMING|ENDED", message = "status must be one of [ONGOING, UPCOMING, ENDED]")
            String status
            )
    {
        return eventComparisonService.getEventsByStatus(status);
    }


}