package com.fairing.fairplay.temp.repository.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.temp.dto.event.EventCompareDto;
import com.fairing.fairplay.temp.dto.event.EventCompareTempDto;
import com.fairing.fairplay.temp.dto.event.Top3EventCompareDto;
import com.fairing.fairplay.ticket.entity.QEventTicket;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EventCompareRepository {

    private final JPAQueryFactory queryFactory;
    private final QEvent e = QEvent.event;
    private final QReservation r = QReservation.reservation;
    private final QEventDetail ed = QEventDetail.eventDetail;
    private final QTicket t = QTicket.ticket;
    private final QEventTicket et = QEventTicket.eventTicket;

    public List<EventCompareDto> getEventComparisonData(Integer status) {
        return getEventComparisonDataWithPaging(status, PageRequest.of(0, 5)).getContent();
    }

    public Page<EventCompareDto> getEventComparisonDataWithPaging(Integer status, Pageable pageable) {
        // 기본 페이지 크기를 5로 설정
        if (pageable == null) {
            pageable = PageRequest.of(0, 5);
        }

        List<EventCompareTempDto> tmp = queryFactory
                .select(Projections.constructor(EventCompareTempDto.class,
                        e.eventId,
                        e.statusCode.eventStatusCodeId,
                        e.titleKr,
                        r.user.userId.countDistinct().castToNum(Long.class).coalesce(0L), // 유저수 수정
                        r.reservationId.countDistinct().castToNum(Long.class).coalesce(0L), // 예약수 수정
                        r.price.multiply(r.quantity).sum().castToNum(BigDecimal.class).coalesce(BigDecimal.ZERO),
                        r.canceled.when(true).then(1).otherwise(0).sum().castToNum(Long.class).coalesce(0L),
                        ed.startDate,
                        ed.endDate,
                        ed.updatedAt))
                .from(e)
                .leftJoin(r).on(r.event.eventId.eq(e.eventId))
                .leftJoin(ed).on(ed.event.eventId.eq(e.eventId))
                .where(status == null ? null : e.statusCode.eventStatusCodeId.eq(status))
                .groupBy(e.eventId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(e.eventId.countDistinct())
                .from(e)
                .leftJoin(ed).on(ed.event.eventId.eq(e.eventId))
                .where(status == null ? null : e.statusCode.eventStatusCodeId.eq(status))
                .fetchOne();

        List<EventCompareDto> result = new ArrayList<>();
        for (EventCompareTempDto dto : tmp) {
            EventCompareDto compareDto = new EventCompareDto();
            compareDto.setEventName(dto.getEventName());
            compareDto.setStatus(dto.getStatus());
            compareDto.setUserCount(dto.getUserCount());
            compareDto.setReservationCount(dto.getReservationCount());
            compareDto.setTotalRevenue(dto.getTotalRevenue());
            compareDto.setAverageTicketPrice(getTicketAvgPrice(dto.getEventId()));
            if (dto.getReservationCount() > 0) {
                compareDto.setCancelRate(dto.getCancelCount().doubleValue() / dto.getReservationCount());
            } else {
                compareDto.setCancelRate(0.0);
            }
            compareDto.setStartDate(dto.getStartDate());
            compareDto.setEndDate(dto.getEndDate());
            compareDto.setModifyDate(dto.getUpdatedAt().toLocalDate());
            result.add(compareDto);
        }

        return new PageImpl<>(result, pageable, total != null ? total : 0);
    }

    public Long getTicketAvgPrice(Long eventId) {
        return queryFactory
                .select(t.price.avg().round().castToNum(Long.class).coalesce(0L))
                .from(t)
                .join(et).on(t.ticketId.eq(et.ticket.ticketId))
                .where(et.event.eventId.eq(eventId))
                .fetchOne();
    }

    public Top3EventCompareDto getTop3EventComparisonList() {
        List<EventCompareDto> top3Events = getEventComparisonData(null);
        top3Events.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        Top3EventCompareDto result = new Top3EventCompareDto();
        result.setTop3Events(top3Events);
        result.setUserCount(top3Events.stream().mapToLong(EventCompareDto::getUserCount).sum());
        result.setReservationCount(top3Events.stream().mapToLong(EventCompareDto::getReservationCount).sum());
        result.setTotalRevenue(
                top3Events.stream().map(EventCompareDto::getTotalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }
}
