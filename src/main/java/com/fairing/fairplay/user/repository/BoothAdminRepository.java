package com.fairing.fairplay.user.repository;

import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothAdminRepository extends JpaRepository<BoothAdmin, Long> {
    Optional<BoothAdmin> findByEmail(String email);
    Optional<BoothAdmin> findByUser(Users user);
}
