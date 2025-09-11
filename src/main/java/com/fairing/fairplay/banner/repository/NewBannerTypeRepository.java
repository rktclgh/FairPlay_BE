package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.NewBannerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewBannerTypeRepository extends JpaRepository<NewBannerType, Long> {

    /**
     * 코드로 배너 타입 조회
     */
    Optional<NewBannerType> findByCode(String code);

    /**
     * 활성화된 배너 타입들만 조회
     */
    List<NewBannerType> findByIsActiveTrue();

    /**
     * 코드로 활성화된 배너 타입 조회
     */
    Optional<NewBannerType> findByCodeAndIsActiveTrue(String code);

    /**
     * 배너 타입명으로 조회
     */
    Optional<NewBannerType> findByName(String name);

    /**
     * 특정 가격 범위의 배너 타입 조회
     */
    @Query("SELECT nbt FROM NewBannerType nbt WHERE nbt.basePrice BETWEEN :minPrice AND :maxPrice AND nbt.isActive = true")
    List<NewBannerType> findByPriceRange(int minPrice, int maxPrice);

    /**
     * 코드 존재 여부 확인
     */
    boolean existsByCode(String code);

    /**
     * 활성화된 배너 타입 개수 조회
     */
    long countByIsActiveTrue();
}