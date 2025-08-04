package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Integer> {
    Optional<RegionCode> findByName(String name);
}