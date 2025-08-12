package com.fairing.fairplay.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fairing.fairplay.admin.entity.FunctionLevel;

public interface FunctionLevelRepository extends JpaRepository<FunctionLevel, Long> {
    Optional<FunctionLevel> findByFunctionName(String functionName);

}
