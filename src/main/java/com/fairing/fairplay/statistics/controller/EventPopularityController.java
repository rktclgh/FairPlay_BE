package com.fairing.fairplay.statistics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.event.EventPopularityPageResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.service.event.EventPopularityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/event-popularity")
@RequiredArgsConstructor
public class EventPopularityController {

    private final EventPopularityService eventPopularityService;

    /**
     * 이벤트 인기 통계 목록 - 페이징 조회
     *
     * @param startDate    조회 시작일 (yyyy-MM-dd)
     * @param endDate      조회 종료일 (yyyy-MM-dd)
     * @param mainCategory 메인 카테고리 (optional)
     * @param subCategory  서브 카테고리 (optional)
     * @param pageable     페이징 정보 (page, size, sort)
     * @return 페이징된 이벤트 인기 통계 DTO 페이지
     */
    @GetMapping("/list")
    @FunctionAuth("getEventAggregatedPopularity")
    public Page<EventPopularityStatisticsListDto> getAggregatedPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String subCategory,
            Pageable pageable) {
        return eventPopularityService.getEventsByCategory(startDate, endDate, mainCategory, subCategory, pageable);
    }

    @GetMapping("/search")
    @FunctionAuth("getSearchPopularity")
    public List<EventPopularityStatisticsListDto> getSearchPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = true) String keyword) {
        return eventPopularityService.searchEvents(startDate, endDate, keyword, mainCategory, subCategory);
    }

    @GetMapping("/report")
    @FunctionAuth("getEventReportPopularity")
    public EventPopularityPageResponseDto getReportPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return eventPopularityService.getPopularityPageData(startDate, endDate);
    }

}
