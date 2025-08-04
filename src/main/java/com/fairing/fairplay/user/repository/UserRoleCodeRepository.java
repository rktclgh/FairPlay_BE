package com.fairing.fairplay.user.repository;

import com.fairing.fairplay.user.entity.UserRoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRoleCodeRepository extends JpaRepository<UserRoleCode, Integer> {
    Optional<UserRoleCode> findByCode(String code);
}
