package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.ticket.entity.ScheduleTicketId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleTicketRepository extends JpaRepository<ScheduleTicket, ScheduleTicketId> {

    void deleteByEventSchedule_ScheduleId(Long scheduleId);

    @Query("""
    SELECT new com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto(
            t.ticketId,
            t.name,
            t.price,
            COALESCE(st.remainingStock, st.saleQuantity, 0),
            st.salesStartAt,
            st.salesEndAt,
            st.visible
        )
        FROM EventTicket et
        JOIN et.ticket t
        JOIN et.event e
        JOIN EventSchedule es ON es.event.eventId = e.eventId
        LEFT JOIN ScheduleTicket st
            ON st.eventSchedule.scheduleId = es.scheduleId
           AND st.ticket.ticketId = t.ticketId
        WHERE e.eventId = :eventId
          AND es.scheduleId = :scheduleId
          AND t.deleted = false
    """)
    List<ScheduleTicketResponseDto> findScheduleTickets(
            @Param("eventId") Long eventId,
            @Param("scheduleId") Long scheduleId
    );

    // 동시성 문제 해결을 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM ScheduleTicket st WHERE st.id = :id")
    Optional<ScheduleTicket> findByIdWithPessimisticLock(@Param("id") ScheduleTicketId id);
    
    // 원자적 재고 차감 (더 안전한 방법)
    @Modifying
    @Query(value = """
        UPDATE schedule_ticket 
        SET remaining_stock = remaining_stock - :quantity 
        WHERE ticket_id = :ticketId AND schedule_id = :scheduleId AND remaining_stock >= :quantity
        """, nativeQuery = true)
    int decreaseStockIfAvailable(@Param("ticketId") Long ticketId, 
                                @Param("scheduleId") Long scheduleId, 
                                @Param("quantity") int quantity);
    
    // 원자적 재고 증가
    @Modifying
    @Query(value = """
        UPDATE schedule_ticket 
        SET remaining_stock = remaining_stock + :quantity 
        WHERE ticket_id = :ticketId AND schedule_id = :scheduleId
        """, nativeQuery = true)
    int increaseStock(@Param("ticketId") Long ticketId, 
                     @Param("scheduleId") Long scheduleId, 
                     @Param("quantity") int quantity);
}