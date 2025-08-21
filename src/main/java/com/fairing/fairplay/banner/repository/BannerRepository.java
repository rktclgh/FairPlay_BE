package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.Banner;
import com.fairing.fairplay.banner.entity.BannerStatusCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // 노출 배너 조회 (타입 지정)
    List<Banner> findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String typeCode, String statusCode, LocalDateTime now1, LocalDateTime now2
    );

    // 노출 배너 조회 (타입 무시)
    List<Banner> findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String code, LocalDateTime now1, LocalDateTime now2
    );


    // 기간 만료된 배너 전체 INACTIVE 처리 (ALL TYPES)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
              update Banner b
                 set b.bannerStatusCode = :inactiveStatus
               where b.bannerStatusCode.code = 'ACTIVE'
                 and b.endDate < :now
            """)
    int deactivateExpiredAll(@Param("now") LocalDateTime now,
                             @Param("inactiveStatus") BannerStatusCode inactiveStatus);

    // 특정 타입 "전체" 비활성화 (필요 시 사용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update Banner b
                   set b.bannerStatusCode = :inactiveStatus
                 where b.bannerType.code = :typeCode
            """)
    int deactivateAllByType(@Param("typeCode") String typeCode,
                            @Param("inactiveStatus") BannerStatusCode inactiveStatus);

    // 특정 타입의 ACTIVE만 전부 비활성화 (create 시 사용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update Banner b
                   set b.bannerStatusCode = :inactiveStatus
                 where b.bannerType.code = :typeCode
                   and b.bannerStatusCode.code = :activeCode
            """)
    int deactivateAllActiveByType(@Param("typeCode") String typeCode,
                                  @Param("activeCode") String activeCode,
                                  @Param("inactiveStatus") BannerStatusCode inactiveStatus);

    //  “하나만 유지”: 자기 자신 제외 ACTIVE 비활성화 (update/status-change 시 사용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update Banner b
                   set b.bannerStatusCode = :inactiveStatus
                 where b.bannerType.code = :typeCode
                   and b.bannerStatusCode.code = :activeCode
                   and b.id <> :excludeId
            """)
    int deactivateOthersActiveByType(@Param("typeCode") String typeCode,
                                     @Param("activeCode") String activeCode,
                                     @Param("inactiveStatus") BannerStatusCode inactiveStatus,
                                     @Param("excludeId") Long excludeId);


    @Query("""
                SELECT DISTINCT b.eventId
                  FROM Banner b
                 WHERE b.bannerStatusCode.code = 'ACTIVE'
                   AND b.bannerType.code IN :typeCodes
                   AND b.eventId IS NOT NULL
            """)
    List<Long> findActiveEventIdsInTypes(@Param("typeCodes") List<String> typeCodes);

    // 중복 방지 체크
    boolean existsByBannerType_CodeAndEventIdAndBannerStatusCode_Code(
            String typeCode, Long eventId, String statusCode
    );

    // (NEW) VIP 검색용: 타입/상태/기간 필터 (이름 검색은 배너→이벤트 연관 없으면 생략)
    @Query("""
SELECT b
FROM Banner b
JOIN b.bannerType bt
WHERE (:type   IS NULL OR bt.code = :type)
  AND (:status IS NULL OR b.bannerStatusCode.code = :status)
  AND (:from   IS NULL OR b.endDate   >= :from)
  AND (:to     IS NULL OR b.startDate <= :to)
  AND (
        :q IS NULL
        OR b.eventId IN (
            SELECT e.eventId
            FROM Event e
            WHERE e.titleKr  LIKE CONCAT('%', :q, '%')
               OR e.titleEng LIKE CONCAT('%', :q, '%')
        )
      )
ORDER BY b.startDate DESC, b.priority ASC
""")
    List<Banner> search(
            @Param("type") String type,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("q") String q
    );


    //  하루 단위 재정렬 잠금: DATE() 대신 범위 비교로
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Banner b
        WHERE b.bannerType.code = :type
          AND b.startDate <= :endOfDay
          AND b.endDate   >= :startOfDay
          AND b.bannerStatusCode.code = 'ACTIVE'
        ORDER BY b.priority ASC
    """)
    List<Banner> lockByTypeAndDate(@Param("type") String type,
                                   @Param("startOfDay") LocalDateTime startOfDay,
                                   @Param("endOfDay") LocalDateTime endOfDay);

    // (HERO 집계용) 타입별 현재 활성 개수
    @Query("""
      SELECT COUNT(b) FROM Banner b
       WHERE b.bannerStatusCode.code='ACTIVE'
         AND b.bannerType.code = :typeCode
         AND :now BETWEEN b.startDate AND b.endDate
    """)
    long countActiveAtByType(@Param("now") LocalDateTime now,
                             @Param("typeCode") String typeCode);

    // (HERO 집계용) 최근 N일 신규 개수 (createdAt 없으면 startDate로 대체)
    @Query("""
      SELECT COUNT(b) FROM Banner b
       WHERE b.bannerType.code = :typeCode
         AND b.startDate >= :cut
    """)
    long countRecentByType(@Param("cut") LocalDateTime cut,
                           @Param("typeCode") String typeCode);

    boolean existsByBannerType_CodeAndEventIdAndBannerStatusCode_CodeAndIdNot(
            String typeCode, Long eventId, String statusCode, Long idNot);

    @Query("""
  SELECT b FROM Banner b
  WHERE b.bannerStatusCode.code = 'ACTIVE'
    AND b.bannerType.code = 'HOT_PICK'
    AND :now BETWEEN b.startDate AND b.endDate
  ORDER BY b.priority ASC, b.id DESC
""")
    List<Banner> findActiveHotPicksOrderByRate(
            @Param("now") java.time.LocalDateTime now,
            org.springframework.data.domain.Pageable pageable
    );

    // (전체 타입) ACTIVE 이고 종료일이 now~until 사이인 배너 수
    long countByBannerStatusCode_CodeAndEndDateBetween(
            String status, LocalDateTime from, LocalDateTime to);

    // (특정 타입) 위와 동일, 타입코드까지 필터
    @Query("""
       select count(b) from Banner b
        where b.bannerStatusCode.code = :status
          and b.bannerType.code = :type
          and b.endDate between :from and :to
    """)
    long countExpiringByType(@Param("status") String status,
                             @Param("type") String typeCode,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);
}
