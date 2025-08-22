package com.fairing.fairplay.temp.controller.reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.temp.dto.reservation.ReservationCategoryStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationEventStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationMonthlyStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationWeeklyStatisticsDto;
import com.fairing.fairplay.temp.repository.reservation.ReservationStatisticsRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservation-statistics")
@RequiredArgsConstructor
public class AdminReservationStatisticsController {

    private final ReservationStatisticsRepository reservationRepository;

    @GetMapping("/get-statistics")
    public ResponseEntity<ReservationStatisticsDto> getStatistics() {
        ReservationStatisticsDto statistics = reservationRepository.getReservationDatas();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-weekly-statistics")

    public ResponseEntity<List<ReservationWeeklyStatisticsDto>> getWeeklyStatistics() {
        List<ReservationWeeklyStatisticsDto> statistics = reservationRepository.getWeeklyDatas();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-monthly-statistics")
    public ResponseEntity<List<ReservationMonthlyStatisticsDto>> getMonthlyStatistics(
            @RequestParam("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<ReservationMonthlyStatisticsDto> statistics = reservationRepository.getReservationDatasByMonth(
                startOfMonth,
                endOfMonth);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-category-statistics")
    public ResponseEntity<List<ReservationCategoryStatisticsDto>> getCategoryStatistics() {
        List<ReservationCategoryStatisticsDto> statistics = reservationRepository.getCategoryDatas();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-event-statistics")
    public ResponseEntity<List<ReservationEventStatisticsDto>> getEventStatistics(
            @RequestParam(value = "category", required = false) Integer categoryId) {
        List<ReservationEventStatisticsDto> statistics = reservationRepository
                .getCategoryDatasByMainCategory(categoryId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-event-statistics-paged")
    public ResponseEntity<Page<ReservationEventStatisticsDto>> getEventStatisticsPaged(
            @RequestParam(value = "category", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ReservationEventStatisticsDto> statistics = reservationRepository
                .getCategoryDatasByMainCategoryWithPaging(categoryId, pageable);
        return ResponseEntity.ok(statistics);
    }
}
