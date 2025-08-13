package com.fairing.fairplay.history.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fairing.fairplay.history.entity.ChangeHistory;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {

}
