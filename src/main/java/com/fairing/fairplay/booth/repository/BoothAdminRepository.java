package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.user.entity.BoothAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothAdminRepository extends JpaRepository<BoothAdmin, Long> {
    Optional<BoothAdmin> findByEmail(String email);
}
