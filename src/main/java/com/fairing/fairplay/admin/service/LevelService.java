package com.fairing.fairplay.admin.service;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.repository.AccountLevelRepository;

@Service
public class LevelService {
    @Autowired
    private AccountLevelRepository accountLevelRepository;

    public BigInteger getAccountLevel(Long userId) {

        return accountLevelRepository.findById(userId)
                .map(accountLevel -> accountLevel.getLevel().toBigInteger())
                .orElse(null);
    }

}
