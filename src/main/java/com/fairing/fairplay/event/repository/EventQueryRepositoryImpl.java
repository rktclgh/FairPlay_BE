package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.EventSummaryDto;
import com.fairing.fairplay.event.dto.QEventSummaryDto;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.ticket.entity.QEventTicket;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
                        detail.startDate,
                        detail.endDate,
                        detail.thumbnailUrl,
                        detail.regionCode.code
                ))
                .from(event)
                .leftJoin(event.eventDetail, detail)
                .leftJoin(event.eventTickets, eventTicket)
                .leftJoin(eventTicket.ticket, ticket)
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
}
