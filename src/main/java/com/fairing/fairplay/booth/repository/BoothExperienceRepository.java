package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothExperience;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoothExperienceRepository extends JpaRepository<BoothExperience, Long> {

    // 부스별 체험 목록 조회
    List<BoothExperience> findByBooth(Booth booth);

    // 부스 ID로 체험 목록 조회
    List<BoothExperience> findByBooth_Id(Long boothId);

    // 특정 날짜의 체험 목록 조회
    List<BoothExperience> findByExperienceDate(LocalDate date);

    // 부스별 특정 날짜 체험 목록 조회
    List<BoothExperience> findByBoothAndExperienceDate(Booth booth, LocalDate date);

    // 예약 가능한 체험 목록 조회 (예약 활성화 상태)
    @Query("SELECT be FROM BoothExperience be WHERE be.isReservationEnabled = true AND be.experienceDate >= :today")
    List<BoothExperience> findAvailableExperiences(@Param("today") LocalDate today);

    // 특정 이벤트의 모든 부스 체험 조회
    @Query("SELECT be FROM BoothExperience be WHERE be.booth.event.eventId = :eventId")
    List<BoothExperience> findByEventId(@Param("eventId") Long eventId);

    // 특정 이벤트의 특정 날짜 체험 조회
    @Query("SELECT be FROM BoothExperience be WHERE be.booth.event.eventId = :eventId AND be.experienceDate = :date")
    List<BoothExperience> findByEventIdAndDate(@Param("eventId") Long eventId, @Param("date") LocalDate date);

    // Pessimistic Lock을 사용한 체험 조회 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})  // 5초 타임아웃
    @Query("SELECT e FROM BoothExperience e WHERE e.experienceId = :experienceId")
    Optional<BoothExperience> findByIdWithPessimisticLock(@Param("experienceId") Long experienceId);
}