package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;

/*@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // 사용자 홈에서 사용할 활성 배너 목록 (현재 시간 기준, 노출 기간 안에 있는 것)
    List<Banner> findAllByBannerStatusCode_CodeAndStartDateBeforeAndEndDateAfterOrderByPriorityDescStartDateDesc(
            String code, LocalDateTime now1, LocalDateTime now2
    );
}*/

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // (1) 타입까지 지정해서 가져오기 (HERO, SEARCH_TOP 등)
    List<Banner> findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String typeCode, String statusCode, LocalDateTime now1, LocalDateTime now2
    );

    // (2) 타입 무시하고 전체 활성 배너 (필요하면 유지)
    List<Banner> findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String code, LocalDateTime now1, LocalDateTime now2
    );

    // (옵션) 오늘 날짜 범위로 빠르게 (00:00~23:59:59)
    @Query("""
      select b from Banner b
      where b.bannerType.code = :typeCode
        and b.bannerStatusCode.code = 'ACTIVE'
        and b.startDate <= :now and b.endDate >= :now
      order by b.priority asc
    """)
    List<Banner> findActiveForNowByType(@Param("typeCode") String typeCode, @Param("now") LocalDateTime now);
}

