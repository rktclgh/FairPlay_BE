package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.NewBannerApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewBannerApplicationRepository extends JpaRepository<NewBannerApplication, Long> {

    /**
     * 호스트용 배너 신청서 목록 조회 (필터링 포함)
     */
    @Query("""
        SELECT nba FROM NewBannerApplication nba
        WHERE nba.applicantId = :userId
        AND (:status IS NULL OR nba.applicationStatus = CAST(:status AS com.fairing.fairplay.banner.entity.NewBannerApplication$ApplicationStatus))
        AND (:bannerTypeCode IS NULL OR EXISTS (
            SELECT 1 FROM NewBannerType nbt 
            WHERE nbt.id = nba.bannerTypeId 
            AND nbt.code = :bannerTypeCode
        ))
        AND (:startDate IS NULL OR DATE(nba.startDate) >= :startDate)
        AND (:endDate IS NULL OR DATE(nba.endDate) <= :endDate)
        ORDER BY nba.createdAt DESC
        """)
    Page<NewBannerApplication> findHostApplicationsWithFilter(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("bannerTypeCode") String bannerTypeCode,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * 관리자용 배너 신청서 목록 조회 (필터링 포함)
     */
    @Query("""
        SELECT nba FROM NewBannerApplication nba
        WHERE (:status IS NULL OR nba.applicationStatus = CAST(:status AS com.fairing.fairplay.banner.entity.NewBannerApplication$ApplicationStatus))
        AND (:bannerTypeCode IS NULL OR EXISTS (
            SELECT 1 FROM NewBannerType nbt 
            WHERE nbt.id = nba.bannerTypeId 
            AND nbt.code = :bannerTypeCode
        ))
        ORDER BY nba.createdAt DESC
        """)
    Page<NewBannerApplication> findAdminApplicationsWithFilter(
        @Param("status") String status,
        @Param("bannerTypeCode") String bannerTypeCode,
        Pageable pageable
    );

    /**
     * 특정 사용자의 모든 배너 신청서 조회
     */
    List<NewBannerApplication> findByApplicantId(Long applicantId);

    /**
     * 특정 사용자의 배너 신청서 개수 조회
     */
    long countByApplicantId(Long applicantId);

    /**
     * 특정 상태의 배너 신청서 조회
     */
    List<NewBannerApplication> findByApplicationStatus(NewBannerApplication.ApplicationStatus status);

    /**
     * 결제 완료된 배너 신청서 조회
     */
    List<NewBannerApplication> findByPaymentStatus(NewBannerApplication.PaymentStatus status);

    /**
     * 특정 이벤트의 배너 신청서 조회
     */
    List<NewBannerApplication> findByEventId(Long eventId);

    /**
     * 특정 기간의 배너 신청서 조회
     */
    @Query("""
        SELECT nba FROM NewBannerApplication nba
        WHERE DATE(nba.startDate) <= :endDate 
        AND DATE(nba.endDate) >= :startDate
        ORDER BY nba.startDate ASC
        """)
    List<NewBannerApplication> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 배너 타입별 신청서 조회
     */
    @Query("""
        SELECT nba FROM NewBannerApplication nba
        JOIN NewBannerType nbt ON nba.bannerTypeId = nbt.id
        WHERE nbt.code = :bannerTypeCode
        ORDER BY nba.createdAt DESC
        """)
    List<NewBannerApplication> findByBannerTypeCode(@Param("bannerTypeCode") String bannerTypeCode);

    /**
     * 승인 대기 중인 신청서 개수 조회 (관리자 대시보드용)
     */
    @Query("SELECT COUNT(nba) FROM NewBannerApplication nba WHERE nba.applicationStatus = 'PENDING'")
    long countPendingApplications();

    /**
     * 결제 대기 중인 신청서 개수 조회 (관리자 대시보드용)
     */
    @Query("""
        SELECT COUNT(nba) FROM NewBannerApplication nba 
        WHERE nba.applicationStatus = 'APPROVED' 
        AND nba.paymentStatus = 'PENDING'
        """)
    long countPaymentPendingApplications();

    /**
     * 활성 배너 개수 조회 (관리자 대시보드용)
     */
    @Query("SELECT COUNT(nba) FROM NewBannerApplication nba WHERE nba.paymentStatus = 'PAID'")
    long countActiveBanners();
}