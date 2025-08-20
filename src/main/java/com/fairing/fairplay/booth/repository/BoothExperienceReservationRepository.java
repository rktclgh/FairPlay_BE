package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothExperience;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 다음 대기 순번 조회 (WAITING 상태만 대상으로 최적화)
    @Query("SELECT COALESCE(MAX(ber.queuePosition), 0) + 1 FROM BoothExperienceReservation ber " +
           "WHERE ber.boothExperience.experienceId = :experienceId AND ber.experienceStatusCode.code = 'WAITING'")
    Integer findNextQueuePosition(@Param("experienceId") Long experienceId);

    // 사용자별 특정 이벤트 예약 목록
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.user = :user " +
           "AND ber.boothExperience.booth.event.eventId = :eventId ORDER BY ber.reservedAt DESC")
    List<BoothExperienceReservation> findByUserAndEventId(@Param("user") Users user, @Param("eventId") Long eventId);

    // 체험별 활성 예약 목록 조회 (삭제 가능 여부 확인용)
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code NOT IN ('CANCELLED', 'COMPLETED')")
    List<BoothExperienceReservation> findActiveReservationsByExperience(@Param("experience") BoothExperience experience);

    // 체험별 진행중인 예약 목록 조회 (수정 가능 여부 확인용)
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code = 'IN_PROGRESS'")
    List<BoothExperienceReservation> findInProgressReservationsByExperience(@Param("experience") BoothExperience experience);

    // 행사 담당자용 예약 목록 조회 (필터링 지원)
    @Query("SELECT ber FROM BoothExperienceReservation ber " +
           "WHERE ber.boothExperience.booth.event.manager.userId = :userId " +
           "AND (:boothId IS NULL OR ber.boothExperience.booth.id = :boothId) " +
           "AND (:reserverName IS NULL OR ber.user.name LIKE %:reserverName%) " +
           "AND (:reserverPhone IS NULL OR ber.user.phone LIKE %:reserverPhone%) " +
           "AND (:experienceDate IS NULL OR ber.boothExperience.experienceDate = :experienceDate) " +
           "AND (:statusCode IS NULL OR ber.experienceStatusCode.code = :statusCode)")
    Page<BoothExperienceReservation> findReservationsForEventManager(
            @Param("userId") Long userId,
            @Param("boothId") Long boothId,
            @Param("reserverName") String reserverName,
            @Param("reserverPhone") String reserverPhone,
            @Param("experienceDate") String experienceDate,
            @Param("statusCode") String statusCode,
            Pageable pageable);

    // 부스 담당자용 예약 목록 조회 (필터링 지원)
    @Query("SELECT ber FROM BoothExperienceReservation ber " +
           "WHERE ber.boothExperience.booth.boothAdmin.userId = :userId " +
           "AND (:boothId IS NULL OR ber.boothExperience.booth.id = :boothId) " +
           "AND (:reserverName IS NULL OR ber.user.name LIKE %:reserverName%) " +
           "AND (:reserverPhone IS NULL OR ber.user.phone LIKE %:reserverPhone%) " +
           "AND (:experienceDate IS NULL OR ber.boothExperience.experienceDate = :experienceDate) " +
           "AND (:statusCode IS NULL OR ber.experienceStatusCode.code = :statusCode)")
    Page<BoothExperienceReservation> findReservationsForBoothManager(
            @Param("userId") Long userId,
            @Param("boothId") Long boothId,
            @Param("reserverName") String reserverName,
            @Param("reserverPhone") String reserverPhone,
            @Param("experienceDate") String experienceDate,
            @Param("statusCode") String statusCode,
            Pageable pageable);

    // 체험별 특정 상태 예약 조회 (상태 코드 문자열로)
    @Query("SELECT ber FROM BoothExperienceReservation ber " +
           "WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code = :statusCode")
    List<BoothExperienceReservation> findByBoothExperienceAndStatusCode(
            @Param("experience") BoothExperience experience, 
            @Param("statusCode") String statusCode);

    // 체험별 대기중+준비완료 예약 목록 (대기 순서대로)
    @Query("SELECT ber FROM BoothExperienceReservation ber WHERE ber.boothExperience = :experience " +
           "AND ber.experienceStatusCode.code IN ('WAITING', 'READY') ORDER BY ber.queuePosition ASC")
    List<BoothExperienceReservation> findWaitingAndReadyReservations(@Param("experience") BoothExperience experience);

    @Query("""
    SELECT r
    FROM BoothExperienceReservation r
    WHERE r.boothExperience.booth.event.eventId = :eventId
      AND r.user.userId = :userId
      AND r.experienceStatusCode.code IN ('WAITING', 'READY', 'IN_PROGRESS')
    ORDER BY r.reservedAt DESC
    """)
    Optional<BoothExperienceReservation> findLatestActiveReservation(
        @Param("eventId") Long eventId,
        @Param("userId") Long userId
    );
}