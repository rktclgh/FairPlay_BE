package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.EventApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventApplyRepository extends JpaRepository<EventApply, Long> {
    
    Page<EventApply> findByStatusCode_CodeOrderByApplyAtDesc(String statusCode, Pageable pageable);
    Page<EventApply> findAllByOrderByApplyAtDesc(Pageable pageable);

    @Query(value = """
            SELECT ea FROM EventApply ea
            JOIN FETCH ea.statusCode sc
            LEFT JOIN FETCH ea.location l
            LEFT JOIN FETCH ea.mainCategory mc
            LEFT JOIN FETCH ea.subCategory sub
            ORDER BY ea.applyAt DESC
            """, countQuery = "SELECT COUNT(ea) FROM EventApply ea")
    Page<EventApply> findAllForResponse(Pageable pageable);

    @Query(value = """
            SELECT ea FROM EventApply ea
            JOIN FETCH ea.statusCode sc
            LEFT JOIN FETCH ea.location l
            LEFT JOIN FETCH ea.mainCategory mc
            LEFT JOIN FETCH ea.subCategory sub
            WHERE sc.code = :statusCode
            ORDER BY ea.applyAt DESC
            """, countQuery = """
            SELECT COUNT(ea) FROM EventApply ea
            JOIN ea.statusCode sc
            WHERE sc.code = :statusCode
            """)
    Page<EventApply> findByStatusCodeForResponse(@Param("statusCode") String statusCode, Pageable pageable);

    @Query("SELECT ea FROM EventApply ea WHERE ea.eventEmail = :eventEmail")
    Optional<EventApply> findByEventEmail(@Param("eventEmail") String eventEmail);

    @Query("""
            SELECT ea FROM EventApply ea
            JOIN FETCH ea.statusCode sc
            LEFT JOIN FETCH ea.location l
            LEFT JOIN FETCH ea.mainCategory mc
            LEFT JOIN FETCH ea.subCategory sub
            WHERE ea.eventEmail = :eventEmail
            """)
    Optional<EventApply> findByEventEmailForResponse(@Param("eventEmail") String eventEmail);
    
    @Query("SELECT COUNT(ea) > 0 FROM EventApply ea WHERE ea.eventEmail = :eventEmail AND ea.statusCode.code = 'PENDING'")
    boolean existsPendingApplicationByEventEmail(String eventEmail);

}
