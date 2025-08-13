package com.fairing.fairplay.statistics.repository.kpistats;

import com.fairing.fairplay.statistics.entity.event.QEventComparisonStatistics;
import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import com.fairing.fairplay.statistics.entity.reservation.QEventDailyStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.fairing.fairplay.user.entity.QUsers;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminKpiStatsCustomRepositoryImpl implements AdminKpiStatsCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public AdminKpiStatistics calculate(LocalDate targetDate) {

        QEventDailyStatistics rstats = QEventDailyStatistics.eventDailyStatistics;
        QEventDailySalesStatistics sstats = QEventDailySalesStatistics.eventDailySalesStatistics;
        QEventComparisonStatistics cstats =QEventComparisonStatistics.eventComparisonStatistics;
        QUsers u = QUsers.users;
        LocalDate start = targetDate.atStartOfDay().toLocalDate();
        LocalDate end = targetDate.plusDays(1).atStartOfDay().toLocalDate();


        Long totalReservation = Long.valueOf(queryFactory
                .select(
                        rstats.reservationCount.sum()
                                .subtract(rstats.cancellationCount.sum())
                )
                .from(rstats)
                .where(rstats.statDate.between(start, end))
                .fetchOne());


        BigDecimal totalSales = BigDecimal.valueOf(queryFactory
                .select(sstats.totalSales.sum()
                        .subtract(sstats.cancelledCount.sum()))
                .from(sstats)
                .where(sstats.statDate.between(start,end))
                .fetchOne());


        Long totalEvents = queryFactory
                .select(cstats.eventId.countDistinct() )
                .from(cstats)
                .where(cstats.startDate.goe(start)
                        .and(cstats.endDate.lt(end)))
                .groupBy(cstats.eventId)
                .fetchOne();

        Long totalUsers  = queryFactory
                .select(u.userId)
                .from(u)
                .where(u.createdAt.between(start.atStartOfDay(), end.atStartOfDay()))
                .fetchOne();

        return AdminKpiStatistics.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalReservations(totalReservation)
                .totalSales(totalSales)
                .statDate(targetDate)
                .build();

    }
}
