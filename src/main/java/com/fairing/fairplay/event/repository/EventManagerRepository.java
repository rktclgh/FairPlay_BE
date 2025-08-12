package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventManagerRepository extends JpaRepository<Event, Long> {

    // EVENT_MANAGER가 담당하는 삭제되지 않은 행사 중 가장 최신 1개 조회
    Optional<Event> findFirstByManager_UserIdAndIsDeletedFalseOrderByEventIdDesc(Long managerId);
}
