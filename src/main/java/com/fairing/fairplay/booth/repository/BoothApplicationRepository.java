package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, Long> {
    // 특정 행사에 대한 부스 신청 목록 조회
    List<BoothApplication> findByEvent_EventId(Long eventId);

    void deleteAllByEvent(Event event);

    @Query("SELECT COUNT(ba) > 0 FROM BoothApplication ba WHERE ba.boothEmail = :boothEmail AND ba.boothApplicationStatusCode.code = 'PENDING'")
    boolean existsPendingApplicationByBoothEmail(String boothEmail);

    // 이메일로 부스 신청 목록 조회 (최신순)
    List<BoothApplication> findByBoothEmailOrderByApplyAtDesc(String boothEmail);

    List<BoothApplication> findByBoothEmail(String boothEmail);

    @EntityGraph(attributePaths = {"event", "boothType", "boothApplicationStatusCode", "boothPaymentStatusCode"})
    @Query("SELECT ba FROM BoothApplication ba WHERE ba.id = :applicationId")
    Optional<BoothApplication> findPaymentPageById(@Param("applicationId") Long applicationId);

    Optional<BoothApplication> findByEvent_EventIdAndBoothEmail(Long eventId, String boothEmail);
}
