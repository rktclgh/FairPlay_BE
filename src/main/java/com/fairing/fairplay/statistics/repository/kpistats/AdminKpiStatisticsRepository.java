package com.fairing.fairplay.statistics.repository.kpistats;

import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdminKpiStatisticsRepository extends JpaRepository<AdminKpiStatistics, Long> {

    List<AdminKpiStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate);

    List<AdminKpiStatistics> findTop7ByStatDateBetweenOrderByStatDateDesc(LocalDate start, LocalDate end);
}
