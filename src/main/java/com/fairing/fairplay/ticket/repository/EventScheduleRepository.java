package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    List<EventSchedule> findAllByEvent_EventId(Long eventId);

    Optional<EventSchedule> findByEvent_EventIdAndScheduleId(Long eventId, Long scheduleId);

    List<EventSchedule> findByEvent_EventId(Long eventId);

    // 동일한 날짜에 시간 겹침 확인 (등록용)
    @Query("SELECT COUNT(es) > 0 FROM EventSchedule es " +
           "WHERE es.event.eventId = :eventId " +
           "AND es.date = :date " +
           "AND ((es.startTime <= :endTime AND es.endTime >= :startTime))")
    boolean existsTimeConflict(@Param("eventId") Long eventId, 
                              @Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime, 
                              @Param("endTime") LocalTime endTime);

    // 동일한 날짜에 시간 겹침 확인 (수정용 - 자기 자신 제외)
    @Query("SELECT COUNT(es) > 0 FROM EventSchedule es " +
           "WHERE es.event.eventId = :eventId " +
           "AND es.scheduleId != :scheduleId " +
           "AND es.date = :date " +
           "AND ((es.startTime <= :endTime AND es.endTime >= :startTime))")
    boolean existsTimeConflictForUpdate(@Param("eventId") Long eventId,
                                       @Param("scheduleId") Long scheduleId,
                                       @Param("date") LocalDate date,
                                       @Param("startTime") LocalTime startTime, 
                                       @Param("endTime") LocalTime endTime);

    // 회차별 티켓 설정 상태 확인 (visible = true인 티켓이 있는지)
    @Query("""
        SELECT es.scheduleId as scheduleId,
               CASE 
                   WHEN EXISTS(SELECT 1 FROM ScheduleTicket st WHERE st.eventSchedule.scheduleId = es.scheduleId AND st.visible = true)
                   THEN true
                   ELSE false
               END as hasActiveTickets
        FROM EventSchedule es
        WHERE es.event.eventId = :eventId
        """)
    List<Map<String, Object>> findScheduleTicketStatus(@Param("eventId") Long eventId);

    // 회차별 판매된 티켓 개수 조회 (QR 티켓 발급 개수)
    @Query("""
        SELECT es.scheduleId as scheduleId,
               COUNT(qr.id) as soldTicketCount
        FROM EventSchedule es
        LEFT JOIN QrTicket qr ON qr.eventSchedule.scheduleId = es.scheduleId AND qr.active = true
        WHERE es.event.eventId = :eventId
        GROUP BY es.scheduleId
        """)
    List<Map<String, Object>> findSoldTicketCounts(@Param("eventId") Long eventId);

}
