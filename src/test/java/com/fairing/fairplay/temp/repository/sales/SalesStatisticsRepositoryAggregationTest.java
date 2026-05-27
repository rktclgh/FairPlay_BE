package com.fairing.fairplay.temp.repository.sales;

import com.fairing.fairplay.temp.dto.sales.DailySalesDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesStatisticsRepositoryAggregationTest {

    @Test
    void dailySalesSumsSameDateAndPaymentTypeAcrossDifferentTimestamps() {
        List<DailySalesDto> results = SalesStatisticsRepository.toDailySalesDtos(List.of(
                new SalesStatisticsRepository.DailySalesRow(
                        LocalDate.of(2026, 5, 27), "티켓", BigDecimal.valueOf(1000), 1L),
                new SalesStatisticsRepository.DailySalesRow(
                        LocalDate.of(2026, 5, 27), "티켓", BigDecimal.valueOf(2500), 2L),
                new SalesStatisticsRepository.DailySalesRow(
                        LocalDate.of(2026, 5, 27), "부스", BigDecimal.valueOf(4000), 1L),
                new SalesStatisticsRepository.DailySalesRow(
                        LocalDate.of(2026, 5, 28), "티켓", BigDecimal.valueOf(700), 1L)
        ));

        assertThat(results).hasSize(2);

        DailySalesDto may27 = results.get(0);
        assertThat(may27.getDate()).isEqualTo(LocalDate.of(2026, 5, 27));
        assertThat(may27.getReservationAmount()).isEqualByComparingTo("3500");
        assertThat(may27.getBoothAmount()).isEqualByComparingTo("4000");
        assertThat(may27.getTotalAmount()).isEqualByComparingTo("7500");
        assertThat(may27.getTotalCount()).isEqualTo(4L);

        DailySalesDto may28 = results.get(1);
        assertThat(may28.getDate()).isEqualTo(LocalDate.of(2026, 5, 28));
        assertThat(may28.getReservationAmount()).isEqualByComparingTo("700");
        assertThat(may28.getTotalCount()).isEqualTo(1L);
    }
}
