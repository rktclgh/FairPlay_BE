package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.EventApply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApplyRepository extends JpaRepository<EventApply, Long> {
}
