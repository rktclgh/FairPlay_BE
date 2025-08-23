package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsListDto;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationSummaryDto;
import com.fairing.fairplay.statistics.dto.reservation.ReservationDailyTrendDto;
import com.fairing.fairplay.statistics.dto.reservation.ReservationMonthlyTrendDto;
import com.fairing.fairplay.statistics.service.reservation.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservation")
@RequiredArgsConstructor
public class AdminReservationDashboardController {

    private final AdminReservationService adminReservationService;

    /**
     * 예약 목록 - 페이징 조회
     *
     * @param startDate    조회 시작일 (yyyy-MM-dd)
     * @param endDate      조회 종료일 (yyyy-MM-dd)
     * @param mainCategory 메인 카테고리 (optional)
     * @param subCategory  서브 카테고리 (optional)
     * @param pageable     페이징 정보 (page, size, sort)
     * @return 페이징된 이벤트 인기 통계 DTO 페이지
     */
    @GetMapping("/list")
    @FunctionAuth("getAggregatedPopularity")
    public Page<AdminReservationStatsListDto> getAggregatedPopularity(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "mainCategory", required = false) String mainCategory,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @PageableDefault(size = 10, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable) {
        return adminReservationService.getEventsByCategory(startDate, endDate, mainCategory, subCategory, pageable);
    }

    @GetMapping("/trend-month")
    @FunctionAuth("getMonthlyTrend")
    public List<ReservationMonthlyTrendDto> getMonthTrend(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return adminReservationService.monthlyTrend(startDate, endDate);
    }

    @GetMapping("/trend-daily")
    @FunctionAuth("getDailyTrend")
    public List<ReservationDailyTrendDto> getDailyTrend(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return adminReservationService.dailyTrend(startDate, endDate);
    }

    @GetMapping("/search")
    @FunctionAuth("searchEvents")
    public Page<AdminReservationStatsListDto> getSearchReservation(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "mainCategory", required = false) String mainCategory,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 10, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable // 기본 페이징 조건
    ) {
        return adminReservationService.searchEvents(startDate, endDate, keyword, mainCategory, subCategory, pageable);
    }

    @GetMapping("/summary")
    @FunctionAuth("getReportPopularity")
    public AdminReservationSummaryDto getReportPopularity(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return adminReservationService.getReservationSummary(startDate, endDate);
    }

}
