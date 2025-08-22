package com.fairing.fairplay.temp.repository.host;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.temp.dto.host.DailyTrendDto;
import com.fairing.fairplay.temp.dto.host.HostEventReservationDto;
import com.fairing.fairplay.temp.dto.host.StockDto;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QScheduleTicket;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HostEventReservationRepository {
    private final JPAQueryFactory queryFactory;
    private final QEvent e = QEvent.event;
    private final QScheduleTicket st = QScheduleTicket.scheduleTicket;
    private final QEventSchedule es = QEventSchedule.eventSchedule;
    private final EventRepository eventRepository;
    private final EventAdminRepository eventAdminRepository;

    public HostEventReservationDto getHostEventReservationStatistics(Long userId) {
        // 이거 하나 반환하게되면 수정해야함............
        List<Event> event = eventRepository.findByManager_User_UserId(userId);
        if (event == null) {
            return null;
        }

        List<StockDto> results = queryFactory
                .select(Projections.constructor(StockDto.class,
                        st.remainingStock,
                        st.saleQuantity))
                .from(st)
                .join(es).on(es.scheduleId.eq(st.eventSchedule.scheduleId))
                .leftJoin(e).on(e.eventId.eq(es.event.eventId))
                .where(e.eventId.eq(event.get(0).getEventId()))
                .fetch();
        Integer totalSaleQuantity = results.stream()
                .mapToInt(StockDto::getSaleQuantity)
                .sum();
        Integer totalRemainingStock = results.stream()
                .mapToInt(StockDto::getRemainingStock)
                .sum();
        List<Double> rates = results.stream()
                .map(stock -> (double) (stock.getSaleQuantity() - stock.getRemainingStock()) * 100
                        / stock.getSaleQuantity())
                .collect(Collectors.toList());
        rates.sort((a, b) -> Double.compare(b, a));

        double topRate = rates.isEmpty() ? 0 : rates.get(0);
        double bottomRate = rates.size() < 2 ? 0 : rates.get(rates.size() - 1);
        return HostEventReservationDto.builder()
                .totalRate((double) (totalSaleQuantity - totalRemainingStock) * 100 / totalSaleQuantity)
                .averageRate(rates.stream().mapToDouble(Double::doubleValue).average().orElse(0))
                .topRate(topRate)
                .bottomRate(bottomRate)
                .build();
    }

    public List<DailyTrendDto> getDailyTrend(Long userId) {
        List<Event> event = eventRepository.findByManager_User_UserId(userId);
        if (event == null) {
            return null;
        }
        List<Tuple> results = queryFactory.select(
                es.date,
                st.remainingStock.sum(),
                st.saleQuantity.sum())
                .from(es)
                .join(st).on(es.scheduleId.eq(st.eventSchedule.scheduleId))
                .where(e.eventId.eq(event.get(0).getEventId()))
                .groupBy(es.date)
                .fetch();

        if (results.isEmpty()) {
            return null;
        }

        return results.stream().map(t -> {
            double rate = (double) (t.get(st.saleQuantity.sum()) - t.get(st.remainingStock.sum())) * 100
                    / t.get(st.saleQuantity.sum());
            return new DailyTrendDto(
                    t.get(es.date),
                    rate);
        }).collect(Collectors.toList());
    }

}
