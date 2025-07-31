package com.fairing.fairplay.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fairing.fairplay.user.entity.EventAdmin;

public interface EventAdminRepository extends JpaRepository<EventAdmin, Long> {
    
}
