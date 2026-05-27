package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSalesStatsCustomRepositoryImplMappingTest {

    @Test
    void buildStatisticsUsesZeroValuesForEmptyAggregateResults() {
        LocalDate statDate = LocalDate.of(2026, 5, 27);

        AdminSalesStatistics stats = AdminSalesStatsCustomRepositoryImpl.buildStatistics(
                statDate,
                null,
                null,
                null,
                null,
                null,
                0L,
                null,
                null,
                null,
                null,
                null);

        assertThat(stats.getStatDate()).isEqualTo(statDate);
        assertThat(stats.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getReservationRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getAdvertisingRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getBoothRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getOtherRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getPaymentCount()).isZero();
        assertThat(stats.getReservationPaymentCount()).isZero();
        assertThat(stats.getAdvertisingPaymentCount()).isZero();
        assertThat(stats.getBoothPaymentCount()).isZero();
        assertThat(stats.getOtherPaymentCount()).isZero();
        assertThat(stats.getAveragePaymentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getCreatedAt()).isNotNull();
    }
}
