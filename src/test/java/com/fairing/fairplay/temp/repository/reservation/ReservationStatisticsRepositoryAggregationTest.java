package com.fairing.fairplay.temp.repository.reservation;

import com.fairing.fairplay.temp.dto.reservation.ReservationMonthlyStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationWeeklyStatisticsDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatisticsRepositoryAggregationTest {

    @Test
    void monthlyStatisticsRollsDatabaseDayBucketsIntoWeekBuckets() {
        List<ReservationMonthlyStatisticsDto> results = ReservationStatisticsRepository.toMonthlyStatistics(List.of(
                new ReservationStatisticsRepository.MonthlyReservationBucket(1, 2),
                new ReservationStatisticsRepository.MonthlyReservationBucket(7, 3),
                new ReservationStatisticsRepository.MonthlyReservationBucket(8, 5),
                new ReservationStatisticsRepository.MonthlyReservationBucket(28, 7),
                new ReservationStatisticsRepository.MonthlyReservationBucket(29, 11)
        ));

        assertThat(results).extracting(ReservationMonthlyStatisticsDto::getWeekNumber)
                .containsExactly(1, 2, 4, 5);
        assertThat(results).extracting(ReservationMonthlyStatisticsDto::getTotalQuantity)
                .containsExactly(5L, 5L, 7L, 11L);
    }

    @Test
    void weeklyStatisticsKeepsDatabaseDateBucketsAndCounts() {
        List<ReservationWeeklyStatisticsDto> results = ReservationStatisticsRepository.toWeeklyStatistics(List.of(
                new ReservationStatisticsRepository.DailyReservationCount(2026, 5, 26, 4L),
                new ReservationStatisticsRepository.DailyReservationCount(2026, 5, 27, 9L)
        ));

        assertThat(results).extracting(ReservationWeeklyStatisticsDto::getDate)
                .containsExactly(LocalDate.of(2026, 5, 26), LocalDate.of(2026, 5, 27));
        assertThat(results).extracting(ReservationWeeklyStatisticsDto::getTotalQuantity)
                .containsExactly(4L, 9L);
    }
}
