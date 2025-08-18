package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.review.dto.PossibleReviewResponseDto;
import com.fairing.fairplay.user.entity.Users;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findByEvent_EventId(Long eventId);

  List<Reservation> findByUser_userId(Long userUserId);

  Optional<Reservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId);

  // 본인 예약 중 관람 일자가 지난 행사 목록 (행사 제목, 건물, 주소, 관람날짜, 요일, 시작시간, 행사시작날짜, 행사종료날짜)
  @Query(
      value = """
              SELECT new com.fairing.fairplay.review.dto.PossibleReviewResponseDto(
                  r.reservationId,
                  new com.fairing.fairplay.review.dto.EventDto(
                    e.titleKr,
                    e.eventDetail.location.buildingName,
                    e.eventDetail.location.address,
                    e.eventDetail.thumbnailUrl,
                    r.schedule.date,
                    COALESCE(r.schedule.weekday, 0),
                    r.schedule.startTime,
                    e.eventDetail.startDate,
                    e.eventDetail.endDate
                  ),
                  t.name
              )
              FROM Reservation r
              JOIN r.event e
              LEFT JOIN r.ticket t
              WHERE r.user.userId = :userId
                AND r.schedule.date < CURRENT_DATE
              ORDER BY r.createdAt DESC
          """,
  countQuery = """
      SELECT COUNT(r)
      FROM Reservation r
      JOIN r.event e
      WHERE r.user.userId = :userId
      AND r.schedule.date < CURRENT_DATE
      """)
  Page<PossibleReviewResponseDto> findPossibleReviewReservationsDto(@Param("userId") Long userId,
      Pageable pageable);


  // 예약자 명단 조회 (페이지네이션 + 필터링)
  @Query(
          value = """
        SELECT r FROM Reservation r
        JOIN FETCH r.user u
        JOIN FETCH r.event e
        LEFT JOIN FETCH r.schedule s
        LEFT JOIN FETCH r.ticket t
        LEFT JOIN FETCH r.reservationStatusCode rsc
        WHERE r.event.eventId = :eventId
          AND (:status IS NULL OR rsc.code = :status)
          AND (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%'))
          AND (:reservationId IS NULL OR r.reservationId = :reservationId)
        ORDER BY r.createdAt DESC
    """,
          countQuery = """
        SELECT COUNT(r) FROM Reservation r
        LEFT JOIN r.user u
        LEFT JOIN r.reservationStatusCode rsc
        WHERE r.event.eventId = :eventId
          AND (:status IS NULL OR rsc.code = :status)
          AND (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%'))
          AND (:reservationId IS NULL OR r.reservationId = :reservationId)
    """
  )
  Page<Reservation> findReservationsWithFilters(
      @Param("eventId") Long eventId,
      @Param("status") String status,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("reservationId") Long reservationId,
      Pageable pageable);


  //  예매율 계산
  @Query(value = """
        SELECT s.event_id,
               CASE WHEN s.total_stock = 0 THEN 0
                    ELSE (c.confirmed_qty * 1.0) / s.total_stock
               END AS booking_rate
          FROM (
                SELECT et.event_id, COALESCE(SUM(t.stock), 0) AS total_stock
                  FROM event_ticket et
                  JOIN ticket t ON t.ticket_id = et.ticket_id
                 GROUP BY et.event_id
               ) s
          LEFT JOIN (
                SELECT r.event_id, COALESCE(SUM(r.quantity), 0) AS confirmed_qty
                  FROM reservation r
                  JOIN reservation_status_code rsc
                    ON rsc.reservation_status_code_id = r.reservation_status_code_id
                 WHERE rsc.code = :confirmedCode
                 GROUP BY r.event_id
               ) c ON c.event_id = s.event_id
         ORDER BY booking_rate DESC
    """, nativeQuery = true)
  List<Object[]> findEventBookingRates(@Param("confirmedCode") String confirmedCode);


    // 예매 수량 상위 이벤트 (CONFIRMED/PAID 등 코드 문자열로 필터)
    @Query(value = """
        SELECT r.event_id            AS eventId,
               COALESCE(SUM(r.quantity), COUNT(*)) AS bookedQty
        FROM reservation r
        JOIN reservation_status_code s
             ON s.reservation_status_code_id = r.reservation_status_code_id
        WHERE s.code IN (:statuses)
          AND r.canceled = 0
        GROUP BY r.event_id
        ORDER BY bookedQty DESC
        """, nativeQuery = true)
    List<Object[]> findEventBookingQuantities(@Param("statuses") List<String> statuses);
  }


