package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventManagerRepository extends JpaRepository<Event, Long> {

    // EVENT_MANAGER가 담당하는 삭제되지 않은 행사 중 가장 최신 1개 조회
    Optional<Event> findFirstByManager_UserIdAndIsDeletedFalseOrderByEventIdDesc(Long managerId);

    List<Event> findByManager_UserId(Long userId);

    @Query("""
            SELECT e FROM Event e
            LEFT JOIN FETCH e.statusCode sc
            LEFT JOIN FETCH e.eventDetail ed
            WHERE e.manager.userId = :userId
            """)
    List<Event> findByManagerUserIdWithStatusAndDetail(@Param("userId") Long userId);
}
