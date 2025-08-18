package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.Banner;
import com.fairing.fairplay.banner.entity.BannerStatusCode;
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
}
