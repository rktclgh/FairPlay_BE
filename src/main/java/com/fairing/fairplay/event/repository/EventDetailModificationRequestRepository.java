package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.EventDetailModificationRequest;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventDetailModificationRequestRepository extends JpaRepository<EventDetailModificationRequest, Long> {
    
    List<EventDetailModificationRequest> findByEventAndStatus_CodeOrderByCreatedAtDesc(Event event, String statusCode);
    
    Page<EventDetailModificationRequest> findByStatus_CodeOrderByCreatedAtDesc(String statusCode, Pageable pageable);
    
    @Query("SELECT r FROM EventDetailModificationRequest r WHERE r.event.eventId = :eventId AND r.status.code = 'PENDING'")
    Optional<EventDetailModificationRequest> findPendingRequestByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT COUNT(r) > 0 FROM EventDetailModificationRequest r WHERE r.event.eventId = :eventId AND r.status.code = 'PENDING'")
    boolean existsPendingRequestByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT r FROM EventDetailModificationRequest r " +
           "WHERE (:status IS NULL OR r.status.code = :status) " +
           "AND (:eventId IS NULL OR r.event.eventId = :eventId) " +
           "AND (:requestedBy IS NULL OR r.requestedBy = :requestedBy) " +
           "ORDER BY r.createdAt DESC")
    Page<EventDetailModificationRequest> findWithFilters(
            @Param("status") String status,
            @Param("eventId") Long eventId,
            @Param("requestedBy") Long requestedBy,
            Pageable pageable);
}