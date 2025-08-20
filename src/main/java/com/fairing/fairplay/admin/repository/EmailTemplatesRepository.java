package com.fairing.fairplay.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.admin.entity.EmailTemplates;

@Repository
public interface EmailTemplatesRepository extends JpaRepository<EmailTemplates, Long> {
    EmailTemplates findByName(String name);
}
