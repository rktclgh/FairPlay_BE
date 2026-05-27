package com.fairing.fairplay.temp.repository.reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.querydsl.core.types.dsl.NumberExpression;
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
                NumberExpression<Integer> reservationDay = r.createdAt.dayOfMonth();
                NumberExpression<Integer> totalQuantity = r.quantity.sum();
                List<Tuple> results = queryFactory
                                .select(reservationDay, totalQuantity)
                                .from(r)
                                .where(r.createdAt.goe(startOfMonth.atStartOfDay())
                                                .and(r.createdAt.lt(endOfMonth.plusDays(1).atStartOfDay())))
                                .groupBy(reservationDay)
                                .orderBy(reservationDay.asc())
                                .fetch();

                return toMonthlyStatistics(results.stream()
                                .map(tuple -> new MonthlyReservationBucket(
                                                tuple.get(reservationDay),
                                                tuple.get(totalQuantity)))
                                .toList());
        }

        static List<ReservationMonthlyStatisticsDto> toMonthlyStatistics(List<MonthlyReservationBucket> buckets) {
                Map<Integer, Long> quantityByWeek = new TreeMap<>();
                for (MonthlyReservationBucket bucket : buckets) {
                        if (bucket.dayOfMonth() == null) {
                                continue;
                        }
                        int weekNumber = ((bucket.dayOfMonth() - 1) / 7) + 1;
                        quantityByWeek.merge(weekNumber, bucket.quantity() != null ? bucket.quantity().longValue() : 0L,
                                        Long::sum);
                }

                return quantityByWeek.entrySet().stream()
                                .map(entry -> new ReservationMonthlyStatisticsDto(entry.getKey(), entry.getValue()))
                                .toList();
        }

        record MonthlyReservationBucket(Integer dayOfMonth, Integer quantity) {
        }

        public List<ReservationWeeklyStatisticsDto> getWeeklyDatas() {
                LocalDateTime current = LocalDateTime.now();
                NumberExpression<Integer> reservationYear = r.createdAt.year();
                NumberExpression<Integer> reservationMonth = r.createdAt.month();
                NumberExpression<Integer> reservationDay = r.createdAt.dayOfMonth();
                NumberExpression<Long> totalQuantity = r.reservationId.count();
                List<Tuple> results = queryFactory
                                .select(reservationYear, reservationMonth, reservationDay, totalQuantity)
                                .from(r)
                                .where(r.createdAt.goe(current.minusDays(7)))
                                .groupBy(reservationYear, reservationMonth, reservationDay)
                                .orderBy(reservationYear.asc(), reservationMonth.asc(), reservationDay.asc())
                                .fetch();

                return toWeeklyStatistics(results.stream()
                                .map(tuple -> new DailyReservationCount(
                                                tuple.get(reservationYear),
                                                tuple.get(reservationMonth),
                                                tuple.get(reservationDay),
                                                tuple.get(totalQuantity)))
                                .toList());
        }

        static List<ReservationWeeklyStatisticsDto> toWeeklyStatistics(List<DailyReservationCount> buckets) {
                Map<LocalDate, Long> countByDate = new TreeMap<>();
                for (DailyReservationCount bucket : buckets) {
                        if (bucket.year() != null && bucket.month() != null && bucket.dayOfMonth() != null) {
                                LocalDate date = LocalDate.of(bucket.year(), bucket.month(), bucket.dayOfMonth());
                                countByDate.merge(date, bucket.totalQuantity() != null ? bucket.totalQuantity() : 0L,
                                                Long::sum);
                        }
                }

                return countByDate.entrySet().stream()
                                .map(entry -> new ReservationWeeklyStatisticsDto(entry.getKey(), entry.getValue()))
                                .toList();
        }

        record DailyReservationCount(Integer year, Integer month, Integer dayOfMonth, Long totalQuantity) {
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
