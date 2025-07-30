package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerStatusCodeRepository extends JpaRepository<BannerStatusCode, Long> {
    Optional<BannerStatusCode> findByCode(String code);
}
