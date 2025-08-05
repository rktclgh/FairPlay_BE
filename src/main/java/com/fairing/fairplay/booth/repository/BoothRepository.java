package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothRepository extends JpaRepository<Booth, Long> {
}