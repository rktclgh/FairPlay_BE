package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface BannerTypeRepository extends JpaRepository<BannerType, Long> {
    Optional<BannerType> findByCode(String code);
}
