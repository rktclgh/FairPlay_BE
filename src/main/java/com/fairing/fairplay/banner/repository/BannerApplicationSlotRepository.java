// BannerApplicationSlotRepository.java
package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerApplication;
import com.fairing.fairplay.banner.entity.BannerApplicationSlot;
import com.fairing.fairplay.banner.entity.BannerSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BannerApplicationSlotRepository extends JpaRepository<BannerApplicationSlot, Long> {

    @Query("select bas.slot.id from BannerApplicationSlot bas where bas.bannerApplication.id = :appId")
    List<Long> findSlotIds(@Param("appId") Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select s
    from BannerSlot s
    where s.id in (
        select bas.slot.id
        from BannerApplicationSlot bas
        where bas.bannerApplication.id = :appId
    )
""")
    List<BannerSlot> lockSlotsByApplication(@Param("appId") Long applicationId);

    List<BannerApplicationSlot> findAllByBannerApplication(BannerApplication bannerApplication);
}
