package com.fairing.fairplay.temp.repository.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.event.entity.QMainCategory;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.temp.dto.reservation.ReservationCategoryStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationEventStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationMonthlyStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationWeeklyStatisticsDto;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationStatisticsRepository {
        private final JPAQueryFactory queryFactory;

        private static QReservation r = QReservation.reservation;
        private static QEventDetail ed = QEventDetail.eventDetail;
        private static QMainCategory mc = QMainCategory.mainCategory;

        public ReservationStatisticsDto getReservationDatas() {
                NumberExpression<Integer> totalAmount = r.price.multiply(r.quantity)
                                .subtract(r.price.multiply(r.canceled.when(true).then(1).otherwise(0))).sum();
                return queryFactory
                                .select(Projections.constructor(ReservationStatisticsDto.class,
                                                r.quantity.sum().castToNum(Long.class).coalesce(0L),
                                                r.canceled.when(true).then(1).otherwise(0).sum().castToNum(Long.class)
                                                                .coalesce(0L),
                                                totalAmount.longValue(),
                                                r.price.avg().coalesce(0.0).longValue()))
                                .from(r)
                                .fetchOne();
        }

        public List<ReservationMonthlyStatisticsDto> getReservationDatasByMonth(LocalDate startOfMonth,
                        LocalDate endOfMonth) {
                return queryFactory
                                .select(Projections.constructor(ReservationMonthlyStatisticsDto.class,
                                                Expressions.numberTemplate(Integer.class,
                                                                "CEILING(DAY({0}) / 7.0)",
                                                                r.createdAt), // weekNumber
                                                r.quantity.sum().castToNum(Long.class).coalesce(0L)))
                                .from(r)
                                .where(r.createdAt.between(startOfMonth.atStartOfDay(),
                                                endOfMonth.plusDays(1).atStartOfDay()))
                                .groupBy(Expressions.numberTemplate(Integer.class,
                                                "CEILING(DAY({0}) / 7.0)",
                                                r.createdAt))
                                .orderBy(Expressions.numberTemplate(Integer.class,
                                                "CEILING(DAY({0}) / 7.0)",
                                                r.createdAt).asc())
                                .fetch();
        }

        public List<ReservationWeeklyStatisticsDto> getWeeklyDatas() {
                LocalDateTime current = LocalDateTime.now();
                List<Tuple> results = queryFactory
                                .select(Expressions.stringTemplate("DATE({0})", r.createdAt), r.count())
                                .from(r)
                                .where(r.createdAt.goe(current.minusDays(7)))
                                .groupBy(Expressions.stringTemplate("DATE({0})", r.createdAt))
                                .fetch();
                return results.stream()
                                .map(tuple -> {
                                        java.sql.Date sqlDate = tuple.get(0, java.sql.Date.class);
                                        LocalDate date = sqlDate.toLocalDate();
                                        return new ReservationWeeklyStatisticsDto(
                                                        date,
                                                        tuple.get(1, Long.class));
                                })
                                .toList();
        }

        public List<ReservationCategoryStatisticsDto> getCategoryDatas() {
                List<Tuple> results = queryFactory
                                .select(ed.mainCategory.groupName, r.quantity.sum())
                                .from(r)
                                .join(r.event.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .groupBy(ed.mainCategory.groupName)
                                .orderBy(r.quantity.sum().desc())
                                .fetch();

                return results.stream()
                                .map(tuple -> {
                                        Integer cnt = tuple.get(1, Integer.class);
                                        return new ReservationCategoryStatisticsDto(
                                                        tuple.get(0, String.class),
                                                        cnt.longValue());
                                })
                                .toList();
        }

        public List<ReservationEventStatisticsDto> getCategoryDatasByMainCategory(Integer categoryId) {
                List<Tuple> results = queryFactory
                                .select(ed.mainCategory.groupName, mc.groupName, r.reservationId.count(),
                                                r.quantity.sum())
                                .from(r)
                                .join(r.event.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .where(mc.groupId.eq(categoryId))
                                .groupBy(ed.mainCategory.groupName)
                                .orderBy(r.quantity.sum().desc())
                                .fetch();

                return results.stream()
                                .map(tuple -> {
                                        Long cnt = tuple.get(2, Long.class);
                                        return new ReservationEventStatisticsDto(
                                                        tuple.get(0, String.class),
                                                        tuple.get(1, String.class),
                                                        cnt.intValue(),
                                                        tuple.get(3, Integer.class));
                                })
                                .toList();
        }

        public Page<ReservationEventStatisticsDto> getCategoryDatasByMainCategoryWithPaging(Integer categoryId,
                        Pageable pageable) {
                Long total = queryFactory
                                .select(ed.mainCategory.groupName.countDistinct())
                                .from(r)
                                .join(r.event.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .where(categoryId != null ? mc.groupId.eq(categoryId) : null)
                                .fetchOne();

                List<Tuple> results = queryFactory
                                .select(ed.mainCategory.groupName, mc.groupName, r.reservationId.count(),
                                                r.quantity.multiply(r.price).sum())
                                .from(r)
                                .join(r.event.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .where(categoryId != null ? mc.groupId.eq(categoryId) : null)
                                .groupBy(ed.mainCategory.groupName, mc.groupName)
                                .orderBy(r.quantity.multiply(r.price).sum().desc())
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

                List<ReservationEventStatisticsDto> content = results.stream()
                                .map(tuple -> {
                                        Long cnt = tuple.get(2, Long.class);
                                        return new ReservationEventStatisticsDto(
                                                        tuple.get(0, String.class),
                                                        tuple.get(1, String.class),
                                                        cnt.intValue(),
                                                        tuple.get(3, Integer.class));
                                })
                                .toList();

                return new PageImpl<>(content, pageable, total != null ? total : 0);
        }

}
