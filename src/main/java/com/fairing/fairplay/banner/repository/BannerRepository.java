package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // 타입까지 지정해서 가져오기 (예: HERO, SEARCH_TOP 등)
    List<Banner> findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String typeCode, String statusCode, LocalDateTime now1, LocalDateTime now2
    );

    // 타입 무시하고 전체 활성 배너
    List<Banner> findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
            String code, LocalDateTime now1, LocalDateTime now2
    );
}

