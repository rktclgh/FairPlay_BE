package com.fairing.fairplay.statistics.controller;


import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsListDto;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationSummaryDto;
import com.fairing.fairplay.statistics.dto.reservation.ReservationDailyTrendDto;
import com.fairing.fairplay.statistics.dto.reservation.ReservationMonthlyTrendDto;
import com.fairing.fairplay.statistics.service.reservation.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<AdminReservationStatsListDto> getAggregatedPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String subCategory,
            Pageable pageable
    ) {
        return adminReservationService.getEventsByCategory(startDate, endDate, mainCategory, subCategory, pageable);
    }

    @GetMapping("/trend-month")
    public List<ReservationMonthlyTrendDto> getMonthTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return  adminReservationService.monthlyTrend(startDate,  endDate);
    }

    @GetMapping("/trend-daily")
    public List<ReservationDailyTrendDto> getDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return  adminReservationService.dailyTrend(startDate,  endDate);
    }


    @GetMapping("/search")
    public List<AdminReservationStatsListDto> getSearchReservation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = true) String keyword
    ) {
        return  adminReservationService.searchEvents(startDate,  endDate, keyword,  mainCategory, subCategory);
    }

    @GetMapping("/summary")
    public AdminReservationSummaryDto getReportPopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
        return adminReservationService.getReservationSummary(startDate, endDate );
    }




}
