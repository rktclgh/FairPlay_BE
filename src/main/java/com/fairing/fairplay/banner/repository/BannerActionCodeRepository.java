package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerActionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerActionCodeRepository extends JpaRepository<BannerActionCode, Long> {
    Optional<BannerActionCode> findByCode(String code);
}
