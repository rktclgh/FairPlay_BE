package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.sales.*;
import com.fairing.fairplay.statistics.service.sales.AdminSalesStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sales")
@RequiredArgsConstructor
class AdminSalesController {

    private final AdminSalesStatisticsService adminSalesStatisticsService;


    @GetMapping("/summary")
    @FunctionAuth("getAdminSalesSummary")
    public AdminSalesSummaryDto getAdminSalesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = true) String type){

        return adminSalesStatisticsService.summarySales(startDate,endDate,type);
    }

    @GetMapping("/type-stats")
    @FunctionAuth("getAdminSalesByType")
    public List<AdminSalesByTypeDto> getAdminSalesByType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){

        return adminSalesStatisticsService.salesTrendByType(startDate,endDate);
    }

    @GetMapping("/trend-month")
    @FunctionAuth("getAdminSalesMonthlyTrend")
    public List<AdminSalesMonthlyTrendDto> getAdminSalesMonthlyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = true) String type){

        return adminSalesStatisticsService.salesMonthlyTrend(startDate,endDate,type);
    }

    @GetMapping("/trend-daily")
    @FunctionAuth("getAdminSalesDailyTrend")
    public List<AdminSalesDailyTrendDto> getAdminSalesDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = true) String type){

        return adminSalesStatisticsService.salesDailyTrend(startDate,endDate,type);
    }

    @GetMapping("/detail-list")
    @FunctionAuth("getAdminSalesDetailList")
    public Page<AdminSalesDetailListDto> getAdminSalesDetailList(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0", value = "page") int pageNo){

        return adminSalesStatisticsService.salesDetailList(startDate,endDate,pageNo);
    }



}
