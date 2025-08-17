package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;

import java.time.LocalDate;
import java.util.List;

public interface AdminSalesStatsCustomRepository {
    AdminSalesStatistics calculate(LocalDate targetDate);
}
