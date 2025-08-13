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

  Optional<Reservation> findByReservationIdAndUser(Long reservationId, Users user);

  // 본인 예약 중 관람 일자가 지난 행사 목록 (행사 제목, 건물, 주소, 관람날짜, 요일, 시작시간, 행사시작날짜, 행사종료날짜)
  @Query(
      value = """
              SELECT new com.fairing.fairplay.review.dto.PossibleReviewResponseDto(
                  r.reservationId,
                  new com.fairing.fairplay.review.dto.EventDto(
                    e.titleKr,
                    e.eventDetail.location.buildingName,
                    e.eventDetail.location.address,
                    r.schedule.date,
                    r.schedule.weekday,
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
}
