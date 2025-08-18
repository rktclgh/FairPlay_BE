package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, Long> {
    // 특정 행사에 대한 부스 신청 목록 조회
    List<BoothApplication> findByEvent_EventId(Long eventId);

    void deleteAllByEvent(Event event);

    @Query("SELECT COUNT(ba) > 0 FROM BoothApplication ba WHERE ba.boothEmail = :boothEmail AND ba.boothApplicationStatusCode.code = 'PENDING'")
    boolean existsPendingApplicationByBoothEmail(String boothEmail);
}
