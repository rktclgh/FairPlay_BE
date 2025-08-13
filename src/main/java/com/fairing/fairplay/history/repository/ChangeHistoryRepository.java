package com.fairing.fairplay.history.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.fairing.fairplay.history.entity.ChangeHistory;

public interface ChangeHistoryRepository
        extends JpaRepository<ChangeHistory, Long>, JpaSpecificationExecutor<ChangeHistory> {

}
