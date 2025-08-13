package com.fairing.fairplay.statistics.repository.kpistats;


import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;

import java.time.LocalDate;
import java.util.List;

public interface AdminKpiStatsCustomRepository {
    AdminKpiStatistics calculate(LocalDate targetDate);
}
