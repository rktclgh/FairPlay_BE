package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.dto.event.TopEventsResponseDto;
import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import com.fairing.fairplay.statistics.entity.event.QEventPopularityStatistics;
import com.fairing.fairplay.wishlist.entity.QWishlist;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.querydsl.core.types.dsl.Expressions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Repository
@RequiredArgsConstructor
public class EventPopularityStatsCustomRepositoryImpl implements EventPopularityStatsCustomRepository{

    private final JPAQueryFactory queryFactory;




    @Override
    public List<EventPopularityStatistics> calculate(LocalDate targetDate) {

        QEvent e = QEvent.event;
        QReservation r = QReservation.reservation;
        QWishlist w = QWishlist.wishlist;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        e.eventId,
                        e.titleKr,
                        e.viewCount.max().coalesce(0),                   // 조회수(누적 조회수)
                        r.reservationId.countDistinct().coalesce(0L),   // 예약 수 (PK 기준)
                        w.wishlistId.countDistinct().coalesce(0L)       // 찜 수 (PK 기준)
                )
                .from(e)
                .leftJoin(r).on(
                        r.event.eventId.eq(e.eventId)
                                .and(r.createdAt.between(start, end))
                )
                .leftJoin(w).on(
                        w.event.eventId.eq(e.eventId)
                                .and(w.createdAt.between(start, end))
                )
                .groupBy(e.eventId, e.titleKr) // viewCount 제거
                .fetch();

        return results.stream()
                .map(t -> EventPopularityStatistics.builder()
                        .eventId(t.get(e.eventId))
                        .eventTitle(t.get(e.titleKr))
                        .viewCount(Long.valueOf(t.get(e.viewCount.max().coalesce(0))))
                        .reservationCount(t.get(r.reservationId.countDistinct().coalesce(0L)))
                        .wishlistCount(t.get(w.wishlistId.countDistinct().coalesce(0L)))
                        .calculatedAt(targetDate.atStartOfDay())
                        .build()
                ).toList();
    }

    @Override
    public Page<EventPopularityStatisticsListDto>  paymentCountPopularity(
            LocalDate startDate,
            LocalDate endDate,
            String mainCategory,
            String subCategory,
            Pageable pageable  // 페이징 정보 추가
    ) {
        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eps.calculatedAt.goe(startDate.atStartOfDay())
                        .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()))
                );

        if (mainCategory != null && !mainCategory.isBlank()) {
            builder.and(d.mainCategory.groupName.stringValue().eq(mainCategory));
        }

        if (subCategory != null && !subCategory.isBlank()) {
            builder.and(d.subCategory.categoryName.stringValue().eq(subCategory));
        }

        // 데이터 조회 쿼리
        List<EventPopularityStatisticsListDto> content = queryFactory
                .select(Projections.constructor(EventPopularityStatisticsListDto.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum().coalesce(0L),
                        eps.reservationCount.sum().coalesce(0L),
                        eps.wishlistCount.sum().coalesce(0L),
                        d.mainCategory,
                        d.subCategory,
                        Expressions.numberTemplate(Integer.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)", eps.viewCount.sum().coalesce(0L)),
                        eps.calculatedAt.max()
                ))
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .where(builder)
                .groupBy(eps.eventId, eps.eventTitle,d.mainCategory, d.subCategory)
                .orderBy(eps.viewCount.sum().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 (group by가 있으므로 countDistinct 사용 권장)
        Long total = queryFactory
                .select(eps.eventId.countDistinct())
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }


    @Override
    public List<EventPopularityStatisticsListDto> aggregatedPopularity(
            LocalDate startDate,
            LocalDate endDate,
            String mainCategory,
            String subCategory
    ) {
        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eps.calculatedAt.goe(startDate.atStartOfDay())
                        .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()))
        );

        if (mainCategory != null && !mainCategory.isBlank()) {
            builder.and(d.mainCategory.groupName.stringValue().eq(mainCategory));
        }

        if (subCategory != null && !subCategory.isBlank()) {
            builder.and(d.subCategory.categoryName.stringValue().eq(subCategory));
        }

        return queryFactory
                .select(Projections.constructor(EventPopularityStatisticsListDto.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum().coalesce(0L),
                        eps.reservationCount.sum().coalesce(0L),
                        eps.wishlistCount.sum().coalesce(0L),
                        d.mainCategory,
                        d.subCategory,
                        Expressions.numberTemplate(Integer.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)",eps.viewCount.sum().coalesce(0L)),
                        eps.calculatedAt.max()
                ))
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .where(builder)
                .groupBy(eps.eventId, eps.eventTitle, d.mainCategory, d.subCategory)

                .groupBy(eps.eventId, eps.eventTitle)
                .orderBy(eps.viewCount.sum().desc())
                .fetch();
    }



    @Override
    public TopEventsResponseDto topAggregatedPopularity(LocalDate startDate, LocalDate endDate) {

        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;

        // 공통 Where 조건
        BooleanExpression dateCondition =
                eps.calculatedAt.goe(startDate.atStartOfDay())
                .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()));

        // Top5 조회수
        List<EventPopularityStatistics> top5View = queryFactory
                .select(Projections.constructor(EventPopularityStatistics.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum(),
                        eps.reservationCount.sum(),
                        eps.wishlistCount.sum(),
                        Expressions.constant(LocalDateTime.now()) // calculatedAt 더미 값
                ))
                .from(eps)
                .where(dateCondition)
                .groupBy(eps.eventId, eps.eventTitle)
                .orderBy(eps.viewCount.sum().desc())
                .limit(5)
                .fetch();

        // Top5 예약수
        List<EventPopularityStatistics> top5Reservation = queryFactory
                .select(Projections.constructor(EventPopularityStatistics.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum(),
                        eps.reservationCount.sum(),
                        eps.wishlistCount.sum(),
                        Expressions.constant(LocalDateTime.now())
                ))
                .from(eps)
                .where(dateCondition)
                .groupBy(eps.eventId, eps.eventTitle)
                .orderBy(eps.reservationCount.sum().desc())
                .limit(5)
                .fetch();

        // Top5 찜수
        List<EventPopularityStatistics> top5Wishlist = queryFactory
                .select(Projections.constructor(EventPopularityStatistics.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum(),
                        eps.reservationCount.sum(),
                        eps.wishlistCount.sum(),
                        Expressions.constant(LocalDateTime.now())
                ))
                .from(eps)
                .where(dateCondition)
                .groupBy(eps.eventId, eps.eventTitle)
                .orderBy(eps.wishlistCount.sum().desc())
                .limit(5)
                .fetch();

        return TopEventsResponseDto.builder()
                .topByViews(top5View)
                .topByReservations(top5Reservation)
                .topByWishlists(top5Wishlist)
                .build();
    }

    @Override
    public List<EventPopularityStatisticsListDto> searchEventWithRank(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory) {
        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eps.calculatedAt.goe(startDate.atStartOfDay())
                        .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()))
                );

        if (keyword != null && !keyword.isBlank()) {
            builder.and(eps.eventTitle.containsIgnoreCase(keyword));
        }
        if (mainCategory != null && !mainCategory.isBlank()) {
            builder.and(d.mainCategory.groupName.stringValue().eq(mainCategory));
        }
        if (subCategory != null && !subCategory.isBlank()) {
            builder.and(d.subCategory.categoryName.stringValue().eq(subCategory));
        }

        return queryFactory
                .select(Projections.constructor(EventPopularityStatisticsListDto.class,
                        eps.popularityId,
                        eps.eventId,
                        eps.eventTitle,
                        eps.viewCount.sum().coalesce(0L),
                        eps.reservationCount.sum().coalesce(0L),
                        eps.wishlistCount.sum().coalesce(0L),
                        d.mainCategory,
                        d.subCategory,
                        Expressions.numberTemplate(Integer.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)", eps.viewCount.sum().coalesce(0L)),
                        eps.calculatedAt.max()
                ))
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .where(builder)
                .groupBy(
                        eps.eventId,
                        eps.eventTitle,
                        d.mainCategory,
                        d.subCategory
                )
                .orderBy(eps.viewCount.sum().desc())
                .fetch();
    }



}