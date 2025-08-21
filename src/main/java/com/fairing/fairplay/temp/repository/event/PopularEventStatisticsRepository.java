package com.fairing.fairplay.temp.repository.event;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.event.entity.QMainCategory;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.temp.dto.event.EventCategoryStatisticsDto;
import com.fairing.fairplay.temp.dto.event.PopularEventStatisticsDto;
import com.fairing.fairplay.temp.dto.event.Top5EventStatisticsDto;
import com.fairing.fairplay.wishlist.entity.QWishlist;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PopularEventStatisticsRepository {
        private final JPAQueryFactory queryFactory;
        private final QEvent e = QEvent.event;
        private final QReservation r = QReservation.reservation;
        private final QWishlist w = QWishlist.wishlist;
        private final QEventDetail ed = QEventDetail.eventDetail;
        private final QMainCategory mc = QMainCategory.mainCategory;

        public PopularEventStatisticsDto getPopularEvents() {
                Tuple result = queryFactory
                                .select(
                                                e.viewCount.avg().coalesce(0.0),
                                                r.quantity.avg().coalesce(0.0),
                                                w.wishlistId.countDistinct().coalesce(0L))
                                .from(e)
                                .leftJoin(r).on(r.event.eventId.eq(e.eventId))
                                .leftJoin(w).on(w.event.eventId.eq(e.eventId))
                                .fetchOne();

                if (result != null) {
                        return PopularEventStatisticsDto.builder()
                                        .averageViewCount(result.get(0, Double.class).longValue())
                                        .averageReservationCount(result.get(1, Double.class).longValue())
                                        .averageWishlistCount(result.get(2, Long.class))
                                        .build();
                }

                return PopularEventStatisticsDto.builder()
                                .averageViewCount(0L)
                                .averageReservationCount(0L)
                                .averageWishlistCount(0L)
                                .build();
        }

        public List<EventCategoryStatisticsDto> getCategoryEventStatistics() {
                List<Tuple> results = queryFactory
                                .select(
                                                ed.mainCategory.groupName,
                                                e.viewCount.sum().castToNum(Long.class).coalesce(0L),
                                                e.eventId.countDistinct().coalesce(0L),
                                                w.wishlistId.countDistinct().coalesce(0L))
                                .from(e)
                                .join(e.eventDetail, ed)
                                .join(ed.mainCategory, mc)
                                .leftJoin(w).on(w.event.eventId.eq(e.eventId))
                                .groupBy(ed.mainCategory.groupName)
                                .orderBy(e.viewCount.sum().desc())
                                .fetch();

                return results.stream()
                                .map(tuple -> EventCategoryStatisticsDto.builder()
                                                .categoryName(tuple.get(0, String.class))
                                                .totalViewCount(tuple.get(1, Long.class))
                                                .totalEventCount(tuple.get(2, Long.class))
                                                .totalWishlistCount(tuple.get(3, Long.class))
                                                .build())
                                .toList();
        }

        public List<Top5EventStatisticsDto> getTop5Events(int code) {
                // code 1: 조회수, code 2: 예약수, code 3: 위시리스트 수
                NumberExpression<Long> target;
                switch (code) {
                        case 1:
                                target = e.viewCount.castToNum(Long.class);
                                break;
                        case 2:
                                target = r.quantity.sum().castToNum(Long.class);
                                break;
                        case 3:
                                target = w.wishlistId.count();
                                break;
                        default:
                                target = e.viewCount.castToNum(Long.class);
                }

                List<Tuple> results = queryFactory
                                .select(
                                                e.titleKr,
                                                target.coalesce(0L))
                                .from(e)
                                .leftJoin(r).on(r.event.eventId.eq(e.eventId))
                                .leftJoin(w).on(w.event.eventId.eq(e.eventId))
                                .groupBy(e.eventId, e.titleKr)
                                .orderBy(target.desc())
                                .limit(5)
                                .fetch();

                return results.stream()
                                .map(tuple -> new Top5EventStatisticsDto(
                                                tuple.get(0, String.class),
                                                tuple.get(1, Long.class)))
                                .toList();

        }

}
