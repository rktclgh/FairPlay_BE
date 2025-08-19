package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.settlement.dto.SettlementAggregationDto;
import com.fairing.fairplay.settlement.dto.SettlementAggregationRevenueDto;
import com.fairing.fairplay.settlement.entity.QSettlement;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.entity.event.QEventPopularityStatistics;
import com.fairing.fairplay.user.entity.QEventAdmin;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SettlementCustomRepositoryImpl implements SettlementCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public SettlementAggregationDto aggregatedCalculate(Long eventId) {

        QPayment payment = QPayment.payment;

        // 환불액을 제외한 매출 계산
        NumberExpression<BigDecimal> netAmount =
                payment.amount.subtract(payment.refundedAmount.coalesce(BigDecimal.ZERO));

        // 전체 합계 먼저 구함
        BigDecimal totalAmount = queryFactory
                .select(netAmount.sum().coalesce(BigDecimal.ZERO))
                .from(payment)
                .where(payment.event.eventId.eq(eventId),
                        payment.paymentStatusCode.code.eq("COMPLETED"))
                .fetchOne();

        return SettlementAggregationDto.builder()
                .totalAmount(totalAmount)
                .build();

    }

    @Override
    public List<SettlementAggregationRevenueDto> aggregatedRevenueCalculate(Long eventId) {

        QPayment payment = QPayment.payment;
        QPaymentTargetType paymentTargetType = QPaymentTargetType.paymentTargetType;

        // (amount - refundedAmount) 계산
        NumberExpression<BigDecimal> netAmount =
                payment.amount.subtract(payment.refundedAmount.coalesce(BigDecimal.ZERO));
        NumberExpression<BigDecimal> netSum = netAmount.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> results = queryFactory
                .select(
                        paymentTargetType.paymentTargetCode,
                        netSum
                )
                .from(payment)
                .join(payment.paymentTargetType, paymentTargetType)
                .where(payment.event.eventId.eq(eventId))
                .groupBy(paymentTargetType.paymentTargetCode)
                .fetch();


        return results.stream()
                .map(tuple -> SettlementAggregationRevenueDto.builder()
                                .revenueType(tuple.get(paymentTargetType.paymentTargetCode))
                                .revenueTypeAmount(tuple.get(netSum))
                                .build())
                .toList();

    }

    @Override
    public Page<Settlement> findAllByAdminUserId(Long userId, Pageable pageable) {
        QSettlement settlement = QSettlement.settlement;
        QEvent event = QEvent.event;
        QEventAdmin eventAdmin = QEventAdmin.eventAdmin;

        // Settlement 조회 (페이징)
        List<Settlement> content = queryFactory
                .selectFrom(settlement)
                .join(settlement.event, event).fetchJoin()      // event 페치 조인
                .join(event.manager, eventAdmin)             // admin 조인
                .where(eventAdmin.userId.eq(userId))            // 조건
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리
        JPQLQuery<Long> countQuery = queryFactory
                .select(settlement.count())
                .from(settlement)
                .join(settlement.event, event)
                .join(event.manager, eventAdmin)
                .where(eventAdmin.userId.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }



}
