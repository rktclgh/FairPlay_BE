package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BannerTypeRepository extends JpaRepository<BannerType, Long> {
    Optional<BannerType> findByCode(String code);

    @Query("select bt.id from BannerType bt where bt.code = :code")
    Long findIdByCode(@Param("code") String code);
}
