package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventQueryRepository {
    Page<Event> findAll(Pageable pageable);

    List<Event> findAllByEventDetail_StartDateAfter(LocalDate today);

    List<Event> findAllByEventDetail_StartDateLessThanEqualAndEventDetail_EndDateGreaterThanEqual(
            LocalDate start, LocalDate end);

    List<Event> findAllByEventDetail_EndDateBefore(LocalDate today);

    Page<Event> findByHiddenFalseAndEventDetailIsNotNull(Pageable pageable);

    // 사용자 ID로 담당 이벤트 조회
    List<Event> findByManager_User_UserId(Long userId);

    Optional<Event> findByEventCode(String eventCode);

    // 종료일이 특정 날짜와 동일한 이벤트 조회
    List<Event> findAllByEventDetail_EndDate(LocalDate date);

    // 생성일 기준 최신순 상위 N개
    List<Event> findAllByOrderByEventDetail_CreatedAtDesc(Pageable pageable);
    List<Event> findByEventDetailIsNotNullOrderByEventDetail_CreatedAtDesc(Pageable pageable);

}
