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
                        rstats.reservationCount.sum()
                                .coalesce(Expressions.constant(0L))
                                .castToNum(Long.class)
                                .subtract(
                                        rstats.cancellationCount.sum()
                                                .coalesce(Expressions.constant(0L))
                                                .castToNum(Long.class)
                                )
                )
                .from(rstats)
                .where(rstats.statDate.eq(targetDate))
                .fetchOne();



        BigDecimal totalSales = queryFactory
                .select(
                // totalSales 및 cancelledSales 각각에 coalesce 적용 후 차감
                        sstats.totalSales.sum().coalesce(0L)
                                .castToNum(BigDecimal.class)
                .subtract(
                sstats.cancelledSales.sum().coalesce(0L)
                        .castToNum(BigDecimal.class)
                )
        .coalesce(Expressions.constant(BigDecimal.ZERO))
                )
        .from(sstats)
                .where(sstats.statDate.eq(targetDate))
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
                .where(
                        u.createdAt.goe(start.atStartOfDay())
                                .and(u.createdAt.lt(end.atStartOfDay()))
                )
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
