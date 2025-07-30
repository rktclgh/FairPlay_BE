package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerLogRepository extends JpaRepository<BannerLog, Long> {
    // 필요 시 배너별 변경 이력 조회 등 추가 가능
}
