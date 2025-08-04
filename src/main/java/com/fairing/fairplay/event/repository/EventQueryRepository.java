package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.EventSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventQueryRepository {
    Page<EventSummaryDto> findEventSummaries(Pageable pageable);
}
