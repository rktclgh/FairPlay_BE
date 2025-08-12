package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.event.entity.ApplyStatusCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ApplyStatusCodeRepository extends JpaRepository<ApplyStatusCode, Integer> {
    @Query("select a.id from ApplyStatusCode a where a.code = :code")
    Integer findIdByCode(@Param("code") String code);
}
