package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.dto.PaymentSearchCriteria;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentStatusCode;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.user.entity.QUsers;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PaymentAdminRepositoryImpl implements PaymentAdminRepository {

    private final JPAQueryFactory queryFactory;

    private static final QPayment payment = QPayment.payment;
    private static final QEvent event = QEvent.event;
    private static final QUsers user = QUsers.users;
    private static final QPaymentTargetType paymentTargetType = QPaymentTargetType.paymentTargetType;
    private static final QPaymentStatusCode paymentStatusCode = QPaymentStatusCode.paymentStatusCode;

    @Override
    public Page<Payment> findPaymentsWithCriteria(PaymentSearchCriteria criteria, Long managerId, Pageable pageable) {
        BooleanBuilder whereClause = buildWhereClause(criteria, managerId);
        
        // 데이터 조회
        List<Payment> payments = queryFactory
                .selectFrom(payment)
                .leftJoin(payment.event, event).fetchJoin()
                .leftJoin(payment.user, user).fetchJoin()
                .leftJoin(payment.paymentTargetType, paymentTargetType).fetchJoin()
                .leftJoin(payment.paymentStatusCode, paymentStatusCode).fetchJoin()
                .where(whereClause)
                .orderBy(createOrderSpecifier(criteria))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 조회
        Long total = queryFactory
                .select(payment.count())
                .from(payment)
                .leftJoin(payment.event, event)
                .leftJoin(payment.user, user)
                .where(whereClause)
                .fetchOne();

        return new PageImpl<>(payments, pageable, total != null ? total : 0L);
    }

    @Override
    public Optional<Payment> findPaymentWithPermissionCheck(Long paymentId, Long managerId) {
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(payment.paymentId.eq(paymentId));
        
        // 권한 체크: EVENT_MANAGER인 경우 managerId 조건 추가
        if (managerId != null) {
            whereClause.and(
                payment.event.isNull() // 이벤트와 무관한 결제 (광고 등)
                .or(payment.event.manager.userId.eq(managerId)) // 또는 본인이 관리하는 이벤트
            );
        }

        Payment result = queryFactory
                .selectFrom(payment)
                .leftJoin(payment.event, event).fetchJoin()
                .leftJoin(payment.user, user).fetchJoin()
                .leftJoin(payment.paymentTargetType, paymentTargetType).fetchJoin()
                .leftJoin(payment.paymentStatusCode, paymentStatusCode).fetchJoin()
                .where(whereClause)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Payment> findPaymentsForExport(PaymentSearchCriteria criteria, Long managerId) {
        BooleanBuilder whereClause = buildWhereClause(criteria, managerId);

        return queryFactory
                .selectFrom(payment)
                .leftJoin(payment.event, event).fetchJoin()
                .leftJoin(payment.user, user).fetchJoin()
                .leftJoin(payment.paymentTargetType, paymentTargetType).fetchJoin()
                .leftJoin(payment.paymentStatusCode, paymentStatusCode).fetchJoin()
                .where(whereClause)
                .orderBy(payment.paidAt.desc())
                .limit(criteria.getSize())
                .fetch();
    }

    @Override
    public Map<String, Long> getPaymentCountStatistics(PaymentSearchCriteria criteria, Long managerId) {
        BooleanBuilder whereClause = buildWhereClause(criteria, managerId);

        // 전체 결제 건수
        Long totalCount = queryFactory
                .select(payment.count())
                .from(payment)
                .leftJoin(payment.event, event)
                .where(whereClause)
                .fetchOne();

        // 완료된 결제 건수
        Long completedCount = queryFactory
                .select(payment.count())
                .from(payment)
                .leftJoin(payment.event, event)
                .where(whereClause.and(payment.paymentStatusCode.code.eq("COMPLETED")))
                .fetchOne();

        // 취소/환불 건수
        Long cancelledCount = queryFactory
                .select(payment.count())
                .from(payment)
                .leftJoin(payment.event, event)
                .where(whereClause.and(
                    payment.paymentStatusCode.code.in("CANCELLED", "REFUNDED")
                ))
                .fetchOne();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCount != null ? totalCount : 0L);
        stats.put("completed", completedCount != null ? completedCount : 0L);
        stats.put("cancelled", cancelledCount != null ? cancelledCount : 0L);
        
        return stats;
    }

    @Override
    public Map<String, BigDecimal> getPaymentAmountStatistics(PaymentSearchCriteria criteria, Long managerId) {
        BooleanBuilder whereClause = buildWhereClause(criteria, managerId);

        // 전체 결제 금액
        BigDecimal totalAmount = queryFactory
                .select(payment.amount.sum())
                .from(payment)
                .leftJoin(payment.event, event)
                .where(whereClause.and(payment.paymentStatusCode.code.eq("COMPLETED")))
                .fetchOne();

        // 환불 금액
        BigDecimal refundedAmount = queryFactory
                .select(payment.refundedAmount.sum())
                .from(payment)
                .leftJoin(payment.event, event)
                .where(whereClause)
                .fetchOne();

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        stats.put("refundedAmount", refundedAmount != null ? refundedAmount : BigDecimal.ZERO);
        stats.put("netAmount", (totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .subtract(refundedAmount != null ? refundedAmount : BigDecimal.ZERO));
        
        return stats;
    }

    @Override
    public Map<String, Object> getPaymentTypeStatistics(PaymentSearchCriteria criteria, Long managerId) {
        BooleanBuilder whereClause = buildWhereClause(criteria, managerId);

        // 결제 타입별 건수 및 금액
        List<Map<String, Object>> typeStats = queryFactory
                .select(Projections.map(
                    paymentTargetType.paymentTargetCode.as("type"),
                    paymentTargetType.paymentTargetName.as("typeName"),
                    payment.count().as("count"),
                    payment.amount.sum().as("amount")
                ))
                .from(payment)
                .leftJoin(payment.event, event)
                .leftJoin(payment.paymentTargetType, paymentTargetType)
                .where(whereClause.and(payment.paymentStatusCode.code.eq("COMPLETED")))
                .groupBy(paymentTargetType.paymentTargetCode, paymentTargetType.paymentTargetName)
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", tuple.get(paymentTargetType.paymentTargetCode));
                    map.put("typeName", tuple.get(paymentTargetType.paymentTargetName));
                    map.put("count", tuple.get(payment.count()));
                    map.put("amount", tuple.get(payment.amount.sum()));
                    return map;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("typeStatistics", typeStats);
        
        return result;
    }

    /**
     * 검색 조건에 따른 WHERE 절 구성
     */
    private BooleanBuilder buildWhereClause(PaymentSearchCriteria criteria, Long managerId) {
        BooleanBuilder whereClause = new BooleanBuilder();

        // 권한 필터링 (EVENT_MANAGER인 경우)
        if (managerId != null) {
            whereClause.and(
                payment.event.isNull() // 이벤트와 무관한 결제 (광고 등)
                .or(payment.event.manager.userId.eq(managerId)) // 또는 본인이 관리하는 이벤트
            );
        }

        // 결제 타입 필터링
        if (criteria.getPaymentTypes() != null && !criteria.getPaymentTypes().isEmpty()) {
            whereClause.and(payment.paymentTargetType.paymentTargetCode.in(criteria.getPaymentTypes()));
        }

        // 결제 상태 필터링
        if (criteria.getPaymentStatuses() != null && !criteria.getPaymentStatuses().isEmpty()) {
            whereClause.and(payment.paymentStatusCode.code.in(criteria.getPaymentStatuses()));
        }

        // 결제일 범위 필터링
        if (criteria.getStartDate() != null) {
            LocalDateTime startDateTime = criteria.getStartDate().atStartOfDay();
            whereClause.and(payment.paidAt.goe(startDateTime));
        }
        if (criteria.getEndDate() != null) {
            LocalDateTime endDateTime = criteria.getEndDate().atTime(23, 59, 59);
            whereClause.and(payment.paidAt.loe(endDateTime));
        }

        // 행사명 검색 (부분 일치)
        if (criteria.getEventName() != null && !criteria.getEventName().trim().isEmpty()) {
            whereClause.and(payment.event.titleKr.containsIgnoreCase(criteria.getEventName().trim()));
        }

        // 구매자명 검색 (부분 일치)
        if (criteria.getBuyerName() != null && !criteria.getBuyerName().trim().isEmpty()) {
            whereClause.and(payment.user.name.containsIgnoreCase(criteria.getBuyerName().trim()));
        }

        // 결제 금액 범위 필터링
        if (criteria.getMinAmount() != null) {
            whereClause.and(payment.amount.goe(BigDecimal.valueOf(criteria.getMinAmount())));
        }
        if (criteria.getMaxAmount() != null) {
            whereClause.and(payment.amount.loe(BigDecimal.valueOf(criteria.getMaxAmount())));
        }

        return whereClause;
    }

    /**
     * 정렬 조건 생성
     */
    private OrderSpecifier<?>[] createOrderSpecifier(PaymentSearchCriteria criteria) {
        Order order = "asc".equalsIgnoreCase(criteria.getDirection()) ? Order.ASC : Order.DESC;
        
        switch (criteria.getSort()) {
            case "amount":
                return new OrderSpecifier[]{ new OrderSpecifier<>(order, payment.amount) };
            case "eventName":
                return new OrderSpecifier[]{ new OrderSpecifier<>(order, payment.event.titleKr) };
            case "buyerName":
                return new OrderSpecifier[]{ new OrderSpecifier<>(order, payment.user.name) };
            case "paymentStatus":
                return new OrderSpecifier[]{ new OrderSpecifier<>(order, payment.paymentStatusCode.name) };
            case "paidAt":
            default:
                return new OrderSpecifier[]{ new OrderSpecifier<>(order, payment.paidAt) };
        }
    }
}