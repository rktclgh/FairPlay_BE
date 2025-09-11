package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.NewBannerSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NewBannerSlotRepository extends JpaRepository<NewBannerSlot, Long> {

    /**
     * 특정 배너 신청서의 슬롯 조회
     */
    List<NewBannerSlot> findByBannerApplicationId(Long bannerApplicationId);

    /**
     * 특정 날짜의 활성 슬롯 조회 (배너 노출용)
     */
    @Query("""
        SELECT nbs FROM NewBannerSlot nbs
        WHERE nbs.slotDate = :date 
        AND nbs.status = 'ACTIVE'
        ORDER BY nbs.priority ASC, nbs.id ASC
        """)
    List<NewBannerSlot> findActiveSlotsByDate(@Param("date") LocalDate date);

    /**
     * 특정 기간의 모든 슬롯 조회 (상태 무관)
     */
    List<NewBannerSlot> findBySlotDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 기간의 활성 슬롯 조회
     */
    @Query("""
        SELECT nbs FROM NewBannerSlot nbs
        WHERE nbs.slotDate BETWEEN :startDate AND :endDate
        AND nbs.status = 'ACTIVE'
        ORDER BY nbs.slotDate ASC, nbs.priority ASC
        """)
    List<NewBannerSlot> findActiveSlotsByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 배너 신청서의 슬롯 삭제
     */
    @Modifying
    @Query("DELETE FROM NewBannerSlot nbs WHERE nbs.bannerApplicationId = :applicationId")
    void deleteByBannerApplicationId(@Param("applicationId") Long applicationId);

    /**
     * 특정 날짜와 우선순위에 활성 슬롯이 있는지 확인
     */
    @Query("""
        SELECT COUNT(nbs) > 0 FROM NewBannerSlot nbs
        WHERE nbs.slotDate = :date 
        AND nbs.priority = :priority 
        AND nbs.status = 'ACTIVE'
        """)
    boolean existsActiveSlotByDateAndPriority(@Param("date") LocalDate date, @Param("priority") int priority);

    /**
     * 만료된 슬롯을 EXPIRED 상태로 변경
     */
    @Modifying
    @Query("""
        UPDATE NewBannerSlot nbs 
        SET nbs.status = 'EXPIRED', nbs.expiredAt = CURRENT_TIMESTAMP
        WHERE nbs.slotDate < :currentDate 
        AND nbs.status = 'ACTIVE'
        """)
    int expireOldSlots(@Param("currentDate") LocalDate currentDate);

    /**
     * 통계용: 상태별 슬롯 개수 조회
     */
    @Query("SELECT nbs.status, COUNT(nbs) FROM NewBannerSlot nbs GROUP BY nbs.status")
    List<Object[]> countSlotsByStatus();

    /**
     * 통계용: 날짜별 활성 슬롯 개수 조회
     */
    @Query("""
        SELECT nbs.slotDate, COUNT(nbs) 
        FROM NewBannerSlot nbs 
        WHERE nbs.status = 'ACTIVE'
        AND nbs.slotDate BETWEEN :startDate AND :endDate
        GROUP BY nbs.slotDate 
        ORDER BY nbs.slotDate ASC
        """)
    List<Object[]> countActiveSlotsByDate(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 배너 신청서의 전체 슬롯 개수 조회
     */
    long countByBannerApplicationId(Long bannerApplicationId);

    /**
     * 오늘 활성화되는 슬롯 조회 (스케줄러용)
     */
    @Query("""
        SELECT nbs FROM NewBannerSlot nbs
        WHERE nbs.slotDate = :today 
        AND nbs.status IN ('RESERVED', 'ACTIVE')
        ORDER BY nbs.priority ASC
        """)
    List<NewBannerSlot> findTodaySlots(@Param("today") LocalDate today);
}