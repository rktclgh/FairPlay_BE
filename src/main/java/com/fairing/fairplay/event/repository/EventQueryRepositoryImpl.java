package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.EventSummaryDto;
import com.fairing.fairplay.event.dto.QEventSummaryDto;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.ticket.entity.QEventTicket;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventQueryRepositoryImpl implements EventQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<EventSummaryDto> findEventSummaries(Pageable pageable) {
        QEvent event = QEvent.event;
        QEventDetail detail = QEventDetail.eventDetail;
        QEventTicket eventTicket = QEventTicket.eventTicket;
        QTicket ticket = QTicket.ticket;

        List<EventSummaryDto> content = queryFactory
                .select(new QEventSummaryDto(
                        event.eventId,
                        event.eventCode,
                        event.hidden,
                        event.titleKr,
                        ticket.price.min(),  // null 허용 (티켓 없을 경우)
                        detail.mainCategory.groupName,
                        detail.location.placeName,
                        detail.location.latitude,
                        detail.location.longitude,
                        detail.startDate,
                        detail.endDate,
                        detail.thumbnailUrl,
                        detail.regionCode.name
                ))
                .from(event)
                .join(event.eventDetail, detail)       // 상세 정보가 있는 이벤트만 조회
                .leftJoin(event.eventTickets, eventTicket)
                .leftJoin(eventTicket.ticket, ticket)
                .where(
                        event.hidden.eq(false),         // 숨겨지지 않은 이벤트만
                        detail.eventDetailId.isNotNull()     // 상세 정보가 등록된 이벤트만
                )
                .groupBy(event.eventId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(event.countDistinct())
                .from(event)
                .leftJoin(event.eventDetail, detail)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<EventSummaryDto> findEventSummariesWithFilters (
            String keyword,
            Integer mainCategoryId,
            Integer subCategoryId,
            String regionName,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        QEvent event = QEvent.event;
        QEventDetail detail = QEventDetail.eventDetail;
        QEventTicket eventTicket = QEventTicket.eventTicket;
        QTicket ticket = QTicket.ticket;

        // 조건 조립
        BooleanBuilder builder = new BooleanBuilder()
                .and(event.hidden.eq(false))    // 숨겨지지 않은 이벤트만
                .and(detail.eventDetailId.isNotNull());  // 상세 정보가 등록된 이벤트만

        // 키워드 필터: 제목 한글 또는 영문
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(
                    event.titleKr.containsIgnoreCase(keyword)
                            .or(event.titleEng.containsIgnoreCase(keyword))
            );
        }

        // 카테고리 ID 필터
        if (mainCategoryId != null) {
            builder.and(detail.mainCategory.groupId.eq(mainCategoryId));
        }

        if (subCategoryId != null) {
            builder.and(detail.subCategory.categoryId.eq(subCategoryId));
        }

        // 지역 필터
        if (regionName != null && !regionName.equals("모든지역")) {
            builder.and(
                    detail.regionCode.name.eq(regionName)
            );
        }

        // 행사 기간 필터
        if (fromDate != null) {
            builder.and(detail.endDate.goe(fromDate));
        }
        if (toDate != null) {
            builder.and(detail.startDate.loe(toDate));
        }

        // 콘텐츠 쿼리
        List<EventSummaryDto> content = queryFactory
                .select(new QEventSummaryDto(
                        event.eventId,
                        event.eventCode,
                        event.hidden,
                        event.titleKr,
                        ticket.price.min(), // 최소 가격 (null 허용)
                        detail.mainCategory.groupName,
                        detail.location.placeName,
                        detail.location.latitude,
                        detail.location.longitude,
                        detail.startDate,
                        detail.endDate,
                        detail.thumbnailUrl,
                        detail.regionCode.name
                ))
                .from(event)
                .join(event.eventDetail, detail)    // 상세 정보가 있는 이벤트만 조회
                .leftJoin(event.eventTickets, eventTicket)
                .leftJoin(eventTicket.ticket, ticket)
                .where(builder)
                .groupBy(event.eventId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(event.eventId.desc())
                .fetch();

        // 카운트 쿼리
        Long total = queryFactory
                .select(event.countDistinct())
                .from(event)
                .join(event.eventDetail, detail)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }



}
