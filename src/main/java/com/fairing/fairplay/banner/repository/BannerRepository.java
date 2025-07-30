package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // 사용자 홈에서 사용할 활성 배너 목록 (현재 시간 기준, 노출 기간 안에 있는 것)
    List<Banner> findAllByBannerStatusCode_CodeAndStartDateBeforeAndEndDateAfterOrderByPriorityDescStartDateDesc(
            String code, LocalDateTime now1, LocalDateTime now2
    );
}
