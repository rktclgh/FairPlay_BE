// BannerSlotRepository.java
package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerSlot;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BannerSlotRepository extends JpaRepository<BannerSlot, Long> {

    // 달력 조회용
    List<BannerSlot> findByBannerType_CodeAndSlotDateBetweenOrderBySlotDateAscPriorityAsc(
            String typeCode, LocalDate from, LocalDate to);

    // 단일 슬롯을 가용 상태에서 잠그기 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select s from BannerSlot s
           where s.bannerType.id = :typeId
             and s.slotDate = :slotDate
             and s.priority = :priority
             and s.status = 'AVAILABLE'
           """)
    Optional<BannerSlot> lockAvailable(@Param("typeId") Long typeId,
                                       @Param("slotDate") LocalDate slotDate,
                                       @Param("priority") Integer priority);

    // 여러 슬롯 LOCK 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update BannerSlot s
          set s.status = :to
        where s.id in :slotIds
          and s.status in :allowedFrom
       """)
    int markSold(@Param("slotIds") List<Long> slotIds,
                 @Param("to") com.fairing.fairplay.banner.entity.BannerSlotStatus to,
                 @Param("allowedFrom") java.util.List<com.fairing.fairplay.banner.entity.BannerSlotStatus> allowedFrom);


    // 만료 락 해제 배치
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update BannerSlot s
              set s.status = 'AVAILABLE',
                  s.lockedBy = null,
                  s.lockedUntil = null
            where s.status = 'LOCKED'
              and s.lockedUntil < CURRENT_TIMESTAMP
           """)
    int releaseExpiredLocks();

    // SOLD 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BannerSlot s set s.status = 'SOLD' where s.id in :slotIds")
    int markSold(@Param("slotIds") List<Long> slotIds);

    // 결제 후 생성된 배너 id 연결 (native가 깔끔)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE banner_slot SET sold_banner_id = :bannerId WHERE slot_id = :slotId", nativeQuery = true)
    int setSoldBanner(@Param("slotId") Long slotId, @Param("bannerId") Long bannerId);
}
