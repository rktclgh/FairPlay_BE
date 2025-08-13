package com.fairing.fairplay.statistics.repository.kpistats;

import com.fairing.fairplay.statistics.entity.event.QEventComparisonStatistics;
import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import com.fairing.fairplay.statistics.entity.reservation.QEventDailyStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.fairing.fairplay.user.entity.QUsers;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
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
        LocalDate start = targetDate;
        LocalDate end = targetDate.plusDays(1);


        Long totalReservation = queryFactory
                .select(
                        rstats.reservationCount.sum().castToNum(Long.class)
                                .subtract(rstats.cancellationCount.sum().castToNum(Long.class))
                                .coalesce(Expressions.constant(0L))
                )
                .from(rstats)
                .where(rstats.statDate.between(start, end))
                .fetchOne();



        BigDecimal totalSales = queryFactory
                .select(
                        sstats.totalSales.sum()
                                .castToNum(BigDecimal.class)
                                .subtract(
                                        sstats.cancelledCount.sum()
                                                .castToNum(BigDecimal.class)
                                )
                                .coalesce(Expressions.constant(BigDecimal.ZERO))
                )
                .from(sstats)
                .where(sstats.statDate.between(start, end))
                .fetchOne();




        Long totalEvents = queryFactory
                .select(cstats.eventId.countDistinct() )
                .from(cstats)
                .where(cstats.startDate.goe(start)
                        .and(cstats.endDate.lt(end)))
                .fetchOne();



        Long totalUsers  = queryFactory
                .select(u.userId.count())
                .from(u)
                .where(u.createdAt.between(start.atStartOfDay(), end.atStartOfDay()))
                .fetchOne();



        return AdminKpiStatistics.builder()
                .totalUsers(totalUsers != null ? totalUsers : 0L)
                .totalEvents(totalEvents != null ? totalEvents : 0L)
                .totalReservations(totalReservation != null ? totalReservation : 0L)
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .statDate(targetDate)
                .build();

    }
}
