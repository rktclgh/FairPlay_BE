package com.fairing.fairplay.statistics.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.event.EventComparisonPageResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventComparisonResponseDto;
import com.fairing.fairplay.statistics.service.event.EventComparisonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/event-comparison")
@RequiredArgsConstructor
public class EventComparisonController {

    private final EventComparisonService eventComparisonService;

    /**
     * 행사별 비교 페이지 전체 데이터 조회
     */
    @GetMapping("/page-data")
    @FunctionAuth("getPageData")
    public EventComparisonPageResponseDto getPageData() {
        return eventComparisonService.getComparisonPageData();
    }

    /**
     * 상태별 필터링
     */
    @GetMapping("/filter/status")
    public List<EventComparisonResponseDto> getEventsByStatus(
            @RequestParam @jakarta.validation.constraints.Pattern(regexp = "ONGOING|UPCOMING|ENDED", message = "status must be one of [ONGOING, UPCOMING, ENDED]") String status) {
        return eventComparisonService.getEventsByStatus(status);
    }

}