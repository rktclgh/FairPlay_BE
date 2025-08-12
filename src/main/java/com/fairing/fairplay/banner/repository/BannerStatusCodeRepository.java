package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerStatusCodeRepository extends JpaRepository<BannerStatusCode, Long> {

    Optional<BannerStatusCode> findByCode(String code);

    @Query("select b.id from BannerStatusCode b where b.code = :code")
    Optional<Long> findIdByCode(@Param("code") String code); // 편의 메서드
}

