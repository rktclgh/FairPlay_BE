package com.fairing.fairplay.review.repository;

import com.fairing.fairplay.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  // 특정 행사 ID에 해당하는 모든 리뷰를 가져오는 쿼리
  // Review → Reservation → ScheduleTicket → EventSchedule → Event
//  @Query("""
//          SELECT r FROM Review r
//          JOIN r.reservation res
//          JOIN res.scheduleTicket st
//          JOIN st.eventSchedule es
//          JOIN es.event e
//          WHERE e.id = :eventId
//      """)
//  Page<Review> findByEventId(@Param("eventId") Long eventId, Pageable pageable);
}
