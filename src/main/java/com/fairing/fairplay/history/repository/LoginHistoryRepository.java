package com.fairing.fairplay.history.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.history.entity.LoginHistory;

@Repository
public interface LoginHistoryRepository
                extends JpaRepository<LoginHistory, Long>, JpaSpecificationExecutor<LoginHistory> {
        List<LoginHistory> findAllByOrderByLoginTimeDesc();
}
