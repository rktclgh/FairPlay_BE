package com.fairing.fairplay.review.repository;

import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.user.entity.Users;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  // 특정 행사 ID에 해당하는 모든 리뷰를 가져오는 쿼리
  // Review → Reservation → ScheduleTicket → EventSchedule → Event
  @Query("""
          SELECT r FROM Review r
          JOIN r.reservation res
          JOIN res.schedule es
          JOIN es.event e
          WHERE e.eventId = :eventId
      """)
  Page<Review> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

  Optional<Review> findByIdAndUser(Long reviewId, Users user);
  Page<Review> findByUser(Users user, Pageable pageable);
  boolean existsByReservationAndUser(Reservation reservation, Users user);
}
