package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, Long> {
    // 특정 행사에 대한 부스 신청 목록 조회
    List<BoothApplication> findByEvent_EventId(Long eventId);
}
