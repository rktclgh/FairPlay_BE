package com.fairing.fairplay.temp.repository.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.temp.dto.event.EventCompareDto;
import com.fairing.fairplay.temp.dto.event.EventCompareTempDto;
import com.fairing.fairplay.temp.dto.event.Top3EventCompareDto;
import com.fairing.fairplay.ticket.entity.QEventTicket;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.Tuple;
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
    private final QPayment p = QPayment.payment;
    private final QEventTicket et = QEventTicket.eventTicket;

    public List<EventCompareDto> getEventComparisonData(Integer status) {
        Page<EventCompareDto> page = getEventComparisonDataWithPaging(status, PageRequest.of(0, 5));
        return page != null ? page.getContent() : new ArrayList<>();
    }

    public Page<EventCompareDto> getEventComparisonDataWithPaging(Integer status, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 5);
        }

        Long total = queryFactory
                .select(e.eventId.countDistinct())
                .from(e)
                .leftJoin(ed).on(ed.event.eventId.eq(e.eventId))
                .where(status == null ? null : e.statusCode.eventStatusCodeId.eq(status))
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        if (pageable.getPageNumber() >= totalPages) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<EventCompareTempDto> tmp = queryFactory
                .select(Projections.constructor(EventCompareTempDto.class,
                        e.eventId,
                        e.statusCode.eventStatusCodeId,
                        e.titleKr,
                        r.user.userId.countDistinct().castToNum(Long.class).coalesce(0L), // 유저수
                        r.reservationId.countDistinct().castToNum(Long.class).coalesce(0L), // 예약수
                        r.price.multiply(r.quantity).sum().castToNum(BigDecimal.class).coalesce(BigDecimal.ZERO), // 매출
                        r.canceled.when(true).then(1L).otherwise(0L).sum().castToNum(Long.class).coalesce(0L), // 취소수
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

        List<EventCompareDto> result = new ArrayList<>();
        for (EventCompareTempDto dto : tmp) {

            if (dto == null) {
                continue;
            }

            EventCompareDto compareDto = new EventCompareDto();
            compareDto.setEventName(dto.getEventName());
            compareDto.setStatus(dto.getStatus());
            compareDto.setUserCount(dto.getUserCount() != null ? dto.getUserCount() : 0L);
            compareDto.setReservationCount(dto.getReservationCount() != null ? dto.getReservationCount() : 0L);
            compareDto.setTotalRevenue(dto.getTotalRevenue() != null ? dto.getTotalRevenue() : BigDecimal.ZERO);
            compareDto.setAverageTicketPrice(getTicketAvgPrice(dto.getEventId()));

            Long reservationCount = dto.getReservationCount();
            Long cancelCount = dto.getCancelCount();
            if (reservationCount != null && reservationCount > 0 && cancelCount != null) {
                compareDto.setCancelRate(cancelCount.doubleValue() / reservationCount);
            } else {
                compareDto.setCancelRate(0.0);
            }

            compareDto.setStartDate(dto.getStartDate());
            compareDto.setEndDate(dto.getEndDate());
            // updatedAt이 null인 경우 처리
            compareDto.setModifyDate(dto.getUpdatedAt() != null ? dto.getUpdatedAt().toLocalDate() : null);
            result.add(compareDto);
        }

        return new PageImpl<>(result, pageable, total != null ? total : 0);
    }

    public Long getTicketAvgPrice(Long eventId) {
        if (eventId == null) {
            return 0L;
        }

        Long avgPrice = queryFactory
                .select(t.price.avg().round().castToNum(Long.class).coalesce(0L))
                .from(t)
                .join(et).on(t.ticketId.eq(et.ticket.ticketId))
                .where(et.event.eventId.eq(eventId))
                .fetchOne();

        return avgPrice != null ? avgPrice : 0L;
    }

}
