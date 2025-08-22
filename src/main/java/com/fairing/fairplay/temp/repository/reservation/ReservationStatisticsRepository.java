package com.fairing.fairplay.temp.repository.reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.event.entity.QMainCategory;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.temp.dto.reservation.ReservationCategoryStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationEventStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationMonthlyStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationStatisticsDto;
import com.fairing.fairplay.temp.dto.reservation.ReservationWeeklyStatisticsDto;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationStatisticsRepository {
        private final JPAQueryFactory queryFactory;

        private static QReservation r = QReservation.reservation;
        private static QEvent e = QEvent.event;
        private static QEventDetail ed = QEventDetail.eventDetail;
        private static QMainCategory mc = QMainCategory.mainCategory;
        private static QPayment p = QPayment.payment;
        private final ReservationRepository reservationRepository;
        private final PaymentRepository paymentRepository;

        public ReservationStatisticsDto getReservationDatas() {
                int paymentCnt = paymentRepository.findAllByPaymentTargetType_PaymentTargetCode("RESERVATION").size();
                int cancel = paymentRepository.findAllByRefundedAmountGreaterThanEqual(BigDecimal.ONE).size();
                BigDecimal totalAmount = paymentRepository.findAllByPaymentTargetType_PaymentTargetCode("RESERVATION")
                                .stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal averageAmount = paymentCnt > 0
                                ? totalAmount.divide(BigDecimal.valueOf(paymentCnt), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                return ReservationStatisticsDto.builder()
                                .totalQuantity(paymentCnt)
                                .canceledCount(cancel)
                                .totalAmount(totalAmount)
                                .averagePrice(averageAmount)
                                .build();

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
                                .select(e.titleKr.countDistinct())
                                .from(e)
                                .join(e.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .join(p).on(p.event.eventId.eq(e.eventId))
                                .where((categoryId != null ? mc.groupId.eq(categoryId) : null),
                                                p.paymentTargetType.paymentTargetTypeId.eq(1L))
                                .fetchOne();

                // 중복 합산을 피하기 위해 결제 합계와 예약 수를 서브쿼리로 분리
                com.fairing.fairplay.payment.entity.QPayment p2 = new com.fairing.fairplay.payment.entity.QPayment(
                                "p2");
                com.fairing.fairplay.reservation.entity.QReservation r2 = new com.fairing.fairplay.reservation.entity.QReservation(
                                "r2");

                var amountExpr = JPAExpressions
                                .select(p2.amount.sum().coalesce(java.math.BigDecimal.ZERO)
                                                .subtract(p2.refundedAmount.sum()
                                                                .coalesce(java.math.BigDecimal.ZERO)))
                                .from(p2)
                                .where(p2.event.eventId.eq(e.eventId)
                                                .and(p2.paymentTargetType.paymentTargetTypeId.eq(1L)));

                var reservationCntExpr = JPAExpressions
                                .select(r2.reservationId.countDistinct())
                                .from(r2)
                                .where(r2.event.eventId.eq(e.eventId));

                List<Tuple> paymentAgg = queryFactory
                                .select(e.titleKr, mc.groupName, reservationCntExpr, amountExpr)
                                .from(e)
                                .join(e.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .where((categoryId != null ? mc.groupId.eq(categoryId) : null))
                                .groupBy(e.titleKr, mc.groupName)
                                .orderBy(
                                                com.querydsl.core.types.dsl.Expressions
                                                                .numberTemplate(Long.class, "({0})", reservationCntExpr)
                                                                .desc())
                                .fetch();

                List<ReservationEventStatisticsDto> content = paymentAgg.stream()
                                .skip(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .map(tuple -> {
                                        java.math.BigDecimal amount = tuple.get(3, java.math.BigDecimal.class);
                                        Long cnt = tuple.get(2, Long.class);
                                        return new ReservationEventStatisticsDto(
                                                        tuple.get(0, String.class),
                                                        tuple.get(1, String.class),
                                                        cnt != null ? cnt.intValue() : 0,
                                                        amount != null ? amount.intValue() : 0);
                                })
                                .toList();

                return new PageImpl<>(content, pageable, total != null ? total : 0);
        }

}
