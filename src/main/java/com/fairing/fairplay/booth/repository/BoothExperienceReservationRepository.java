package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothExperience;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoothExperienceReservationRepository extends JpaRepository<BoothExperienceReservation, Long> {

    // 체험별 예약 목록 조회 (예약 시간 순)
    List<BoothExperienceReservation> findByBoothExperienceOrderByReservedAt(BoothExperience boothExperience);

    // 사용자별 예약 목록 조회 (최신순)
    List<BoothExperienceReservation> findByUserOrderByReservedAtDesc(Users user);

    // 특정 상태의 예약 목록 조회
    List<BoothExperienceReservation> findByExperienceStatusCode(BoothExperienceStatusCode statusCode);

    // 체험별 특정 상태 예약 목록 조회
    List<BoothExperienceReservation> findByBoothExperienceAndExperienceStatusCode(
            BoothExperience boothExperience, BoothExperienceStatusCode statusCode);

    // 사용자의 특정 체험 예약 조회 (중복 예약 체크용)
    Optional<BoothExperienceReservation> findByBoothExperienceAndUser(BoothExperience boothExperience, Users user);

    // 사용자의 활성 예약 조회 (취소되지 않은 예약)
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.user = :user " +
           "AND ber.experienceStatusCode.code != 'CANCELLED' AND ber.experienceStatusCode.code != 'COMPLETED'")
    List<BoothExperienceReservation> findActiveReservationsByUser(@Param("user") Users user);

    // 체험별 대기중인 예약 목록 (대기 순서대로)
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code IN ('WAITING', 'READY') ORDER BY ber.queuePosition ASC")
    List<BoothExperienceReservation> findWaitingReservations(@Param("experience") BoothExperience experience);

    // 체험별 현재 체험중인 예약 목록
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code = 'IN_PROGRESS'")
    List<BoothExperienceReservation> findInProgressReservations(@Param("experience") BoothExperience experience);

    // 다음 대기 순번 조회
    @Query("SELECT COALESCE(MAX(ber.queuePosition), 0) + 1 FROM BoothExperienceReservation ber " +
           "WHERE ber.boothExperience = :experience AND ber.experienceStatusCode.code IN ('WAITING', 'READY')")
    Integer findNextQueuePosition(@Param("experience") BoothExperience experience);

    // 사용자별 특정 이벤트 예약 목록
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.user = :user " +
           "AND ber.boothExperience.booth.event.eventId = :eventId ORDER BY ber.reservedAt DESC")
    List<BoothExperienceReservation> findByUserAndEventId(@Param("user") Users user, @Param("eventId") Long eventId);
}