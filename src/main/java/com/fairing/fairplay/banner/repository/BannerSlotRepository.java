package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerSlot;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
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
             and s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.AVAILABLE
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
    int updateStatusIfCurrentIn(
                 @Param("slotIds") List<Long> slotIds,
                 @Param("to") com.fairing.fairplay.banner.entity.BannerSlotStatus to,
                 @Param("allowedFrom") java.util.List<com.fairing.fairplay.banner.entity.BannerSlotStatus> allowedFrom);


    // 만료 락 해제 배치
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update BannerSlot s
                  set s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.AVAILABLE,
                          s.lockedBy = null,
                          s.lockedUntil = null
                  where s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.LOCKED
                        and s.lockedUntil < CURRENT_TIMESTAMP
           """)
    int releaseExpiredLocks();

    // 슬롯에서 SOLD된(=결제완료) 노출 2칸 조회용
    @Query("""
       select b.eventId as eventId, s.priority as priority
         from BannerSlot s
         join s.soldBanner b
        where s.bannerType.code = :typeCode
          and s.slotDate = :slotDate
          and s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.SOLD
        order by s.priority asc
    """)
    List<FixedRow> findSoldFixedRows(@Param("typeCode") String typeCode,
                                     @Param("slotDate") java.time.LocalDate slotDate);

    interface FixedRow {
        Long getEventId();
        Integer getPriority();
    }

    @Query("""
       select s from BannerSlot s
       join fetch s.bannerType bt
       where s.id in :ids
    """)
    List<BannerSlot> findAllWithType(@Param("ids") List<Long> ids);

    // 결제 후 생성된 배너 id 연결 (native가 깔끔)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE banner_slot SET sold_banner_id = :bannerId WHERE slot_id = :slotId", nativeQuery = true)
    int setSoldBanner(@Param("slotId") Long slotId, @Param("bannerId") Long bannerId);

    // HERO 매출(슬롯 SOLD 합계) — JPQL 버전(권장: null은 서비스에서 처리)
    @Query("""
select SUM(s.price)
from BannerSlot s
where s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.SOLD
  and s.bannerType.code = :typeCode
""")
    Long sumSoldAmountByType(@Param("typeCode") String typeCode);

    // (옵션) 기간 필터 버전
    @Query("""
select SUM(s.price)
from BannerSlot s
where s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.SOLD
  and s.bannerType.code = :typeCode
  and s.slotDate between :from and :to
""")
    Long sumSoldAmountByTypeBetween(@Param("typeCode") String typeCode,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    // 만료된 LOCK 상태를 AVAILABLE로 변경
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update BannerSlot s
                  set s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.AVAILABLE,
                          s.lockedBy = null,
                          s.lockedUntil = null
                  where s.status = com.fairing.fairplay.banner.entity.BannerSlotStatus.LOCKED
                        and s.lockedUntil < CURRENT_TIMESTAMP
           """)
    int updateExpiredLocksToAvailable();

    // banner_application_slot에 해당 슬롯이 이미 있는지 확인
    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM banner_application_slot bas
            WHERE bas.slot_id = :slotId
            """, nativeQuery = true)
    boolean existsInApplicationSlot(@Param("slotId") Long slotId);

    // banner_application_slot에 이미 있는 모든 슬롯 ID들 조회
    @Query(value = """
            SELECT DISTINCT bas.slot_id
            FROM banner_application_slot bas
            """, nativeQuery = true)
    List<Long> findReservedSlotIds();
    
    // 특정 배너 ID와 연결된 슬롯들 조회 (하드 딜리트용)
    List<BannerSlot> findBySoldBanner_Id(Long bannerId);

}
