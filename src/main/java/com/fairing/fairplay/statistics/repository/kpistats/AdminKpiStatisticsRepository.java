package com.fairing.fairplay.statistics.repository.kpistats;

import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdminKpiStatisticsRepository extends JpaRepository<AdminKpiStatistics, Long> {

    List<AdminKpiStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate);

    List<AdminKpiStatistics> findTop7ByStatDateBetweenOrderByStatDateDesc(LocalDate start, LocalDate end);

    Optional<Object> findByStatDate(LocalDate date);

    @Modifying
    @Query(value = """
    INSERT INTO admin_kpi_statistics (stat_date, total_events, total_users, total_reservations, total_sales) 
    VALUES (:#{#stats.statDate}, :#{#stats.totalEvents}, :#{#stats.totalUsers}, :#{#stats.totalReservations}, :#{#stats.totalSales})
    ON DUPLICATE KEY UPDATE 
        total_events = VALUES(total_events),
        total_users = VALUES(total_users),
        total_reservations = VALUES(total_reservations),
        total_sales = VALUES(total_sales)
    """, nativeQuery = true)
    void upsertAdminKpiStatistics(@Param("stats") AdminKpiStatistics stats);
}
