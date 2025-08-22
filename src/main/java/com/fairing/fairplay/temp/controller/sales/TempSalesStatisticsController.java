package com.fairing.fairplay.temp.controller.sales;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.settlement.util.ExcelExporter;
import com.fairing.fairplay.temp.dto.sales.AllSalesDto;
import com.fairing.fairplay.temp.dto.sales.DailySalesDto;
import com.fairing.fairplay.temp.dto.sales.TotalSalesStatisticsDto;
import com.fairing.fairplay.temp.repository.sales.SalesStatisticsRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales-statistics/")
@RequiredArgsConstructor
public class TempSalesStatisticsController {
    private final SalesStatisticsRepository salesStatisticsRepository;

    @GetMapping("/total")
    public TotalSalesStatisticsDto getTotalSalesStatistics() {
        return salesStatisticsRepository.getTotalSalesStatistics();
    }

    @GetMapping("/daily-sales")
    public List<DailySalesDto> getDailySales(@RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        return salesStatisticsRepository.getDailySales(startDate, endDate);
    }

    @GetMapping("/compare")
    public DailySalesDto getComparedSales() {
        return salesStatisticsRepository.getCompare();
    }

    @GetMapping("/all-sales")
    public Page<AllSalesDto> getAllSales(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return salesStatisticsRepository.getAllSales(pageable);
    }

    /**
     * 일별 매출 데이터 Excel 내보내기
     * 프론트엔드의 유연한 날짜 필터링 지원
     */
    @GetMapping("/export/daily-sales")
    public ResponseEntity<byte[]> exportDailySales(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
            throws IOException {

        List<DailySalesDto> salesData = salesStatisticsRepository.getDailySales(startDate, endDate);
        byte[] excelFile = ExcelExporter.exportDailySalesList(salesData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        String filename = String.format("daily_sales_%s.xlsx",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
    }

}
