package com.fairing.fairplay.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fairing.fairplay.admin.entity.AdminAccount;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {
}
