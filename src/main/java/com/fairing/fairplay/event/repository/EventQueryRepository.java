package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.EventSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface EventQueryRepository {
    Page<EventSummaryDto> findEventSummaries(Pageable pageable);

    Page<EventSummaryDto> findEventSummariesWithFilters (
            String keyword,
            Integer mainCategoryId,
            Integer subCategoryId,
            String region,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );
}
