package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothTypeRepository extends JpaRepository<BoothType, Long> {
    Optional<BoothType> findById(Long id);
}
