package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.dto.sales.RawSalesData;
import java.time.LocalDate;
import java.util.List;

public interface SalesRawDataRepository {
    List<RawSalesData> fetchSalesData(LocalDate targetDate);
}
