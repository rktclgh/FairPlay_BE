package com.fairing.fairplay.statistics.repository.salesstats;


import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdminSalesStatisticsRepository extends JpaRepository<AdminSalesStatistics,Long> {
    List<AdminSalesStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate);
    Page<AdminSalesStatistics> findByStatDateBetweenOrderByStatDate(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
