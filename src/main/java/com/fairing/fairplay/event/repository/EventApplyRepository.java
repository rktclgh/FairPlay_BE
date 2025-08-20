package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.EventApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventApplyRepository extends JpaRepository<EventApply, Long> {
    
    Page<EventApply> findByStatusCode_CodeOrderByApplyAtDesc(String statusCode, Pageable pageable);
    Page<EventApply> findAllByOrderByApplyAtDesc(Pageable pageable);

    @Query("SELECT ea FROM EventApply ea WHERE ea.eventEmail = :eventEmail")
    Optional<EventApply> findByEventEmail(String eventEmail);
    
    @Query("SELECT COUNT(ea) > 0 FROM EventApply ea WHERE ea.eventEmail = :eventEmail AND ea.statusCode.code = 'PENDING'")
    boolean existsPendingApplicationByEventEmail(String eventEmail);

}
