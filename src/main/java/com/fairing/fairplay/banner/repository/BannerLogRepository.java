package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerLogRepository extends JpaRepository<BannerLog, Long> {
    // 필요 시 배너별 변경 이력 조회 등 추가 가능
    
    // 배너 하드 딜리트 시 관련 로그 삭제용
    int deleteByBanner_Id(Long bannerId);
}
