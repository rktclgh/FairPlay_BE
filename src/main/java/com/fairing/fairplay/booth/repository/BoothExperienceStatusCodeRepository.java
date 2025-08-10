package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoothExperienceStatusCodeRepository extends JpaRepository<BoothExperienceStatusCode, Integer> {

    // 상태 코드로 조회
    Optional<BoothExperienceStatusCode> findByCode(String code);
}