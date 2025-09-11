package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.NewBannerLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewBannerLogRepository extends JpaRepository<NewBannerLog, Long> {

    /**
     * 특정 배너 신청서의 로그 조회
     */
    List<NewBannerLog> findByBannerApplicationIdOrderByCreatedAtDesc(Long bannerApplicationId);

    /**
     * 특정 관리자의 액션 로그 조회
     */
    List<NewBannerLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    /**
     * 특정 액션 타입의 로그 조회
     */
    List<NewBannerLog> findByActionTypeOrderByCreatedAtDesc(String actionType);

    /**
     * 특정 기간의 로그 조회
     */
    @Query("""
        SELECT nbl FROM NewBannerLog nbl
        WHERE nbl.createdAt BETWEEN :startDate AND :endDate
        ORDER BY nbl.createdAt DESC
        """)
    List<NewBannerLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 관리자 액션 통계 (대시보드용)
     */
    @Query("""
        SELECT nbl.actionType, COUNT(nbl)
        FROM NewBannerLog nbl
        WHERE nbl.createdAt >= :fromDate
        GROUP BY nbl.actionType
        ORDER BY COUNT(nbl) DESC
        """)
    List<Object[]> getActionStatistics(@Param("fromDate") LocalDateTime fromDate);

    /**
     * 특정 배너 신청서의 최근 로그 조회
     */
    @Query("""
        SELECT nbl FROM NewBannerLog nbl
        WHERE nbl.bannerApplicationId = :applicationId
        ORDER BY nbl.createdAt DESC
        LIMIT 1
        """)
    NewBannerLog findLatestLogByApplicationId(@Param("applicationId") Long applicationId);

    /**
     * 페이지네이션된 모든 로그 조회 (관리자용)
     */
    Page<NewBannerLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 상태 변경 로그 조회
     */
    @Query("""
        SELECT nbl FROM NewBannerLog nbl
        WHERE nbl.oldStatus = :oldStatus 
        AND nbl.newStatus = :newStatus
        ORDER BY nbl.createdAt DESC
        """)
    List<NewBannerLog> findByStatusChange(
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus
    );
}