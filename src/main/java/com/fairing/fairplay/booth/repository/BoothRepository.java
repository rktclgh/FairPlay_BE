package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoothRepository extends JpaRepository<Booth, Long> {

    List<Booth> findAllByEvent(Event event);
    List<Booth> findByEventAndIsDeletedFalse(Event event);

    // EVENT_MANAGER: 관리하는 행사의 모든 부스 조회
    @Query("SELECT b FROM Booth b " +
           "JOIN FETCH b.event e " +
           "WHERE e.manager.userId = :userId " +
           "ORDER BY b.boothTitle ASC")
    List<Booth> findByEventManagerId(@Param("userId") Long userId);

    // BOOTH_MANAGER: 관리하는 부스만 조회
    @Query("SELECT b FROM Booth b " +
           "JOIN FETCH b.event e " +
           "LEFT JOIN FETCH b.boothAdmin ba " +
           "LEFT JOIN FETCH ba.user u " +
           "WHERE b.boothAdmin.userId = :userId " +
           "ORDER BY b.boothTitle ASC")
    List<Booth> findByBoothAdminId(@Param("userId") Long userId);
    
    // eventId로 모든 부스 조회 (EVENT_MANAGER용 대안)
    @Query("SELECT b FROM Booth b " +
           "JOIN FETCH b.event e " +
           "LEFT JOIN FETCH b.boothAdmin ba " +
           "LEFT JOIN FETCH ba.user u " +
           "WHERE e.eventId = :eventId " +
           "ORDER BY b.boothTitle ASC")
    List<Booth> findByEventId(@Param("eventId") Long eventId);
}