package com.fairing.fairplay.statistics.repository.dailystats;



import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsByCategoryDto;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsListDto;
import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatsCustomRepository {
    List<EventDailyStatistics> calculate(LocalDate targetDate);
    List<AdminReservationStatsByCategoryDto> aggregatedReservationByCategory(
            LocalDate startDate,
            LocalDate endDate

    );

    Page<AdminReservationStatsListDto> searchEventReservationWithRank(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory, Pageable pageable);

    Page<AdminReservationStatsListDto> aggregatedPopularity(
            LocalDate startDate,
            LocalDate endDate,
            String mainCategory,
            String subCategory,
            Pageable pageable  // 페이징 정보 추가
    );
}
