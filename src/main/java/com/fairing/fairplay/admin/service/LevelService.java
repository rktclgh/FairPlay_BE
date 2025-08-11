package com.fairing.fairplay.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.service.UserService;

@Service
public class LevelService {
    @Autowired
    private AccountLevelRepository accountLevelRepository;

    @Autowired
    private FunctionLevelRepository functionLevelRepository;

    public Long getAccountLevel(Long userId) {

        return accountLevelRepository.findById(userId)
                .map(accountLevel -> accountLevel.getLevel())
                .orElse(null);
    }

    public Long getFunctionLevel(String function) {
        return functionLevelRepository.findByFunctionName(function)
                .map(functionLevel -> functionLevel.getLevel())
                .orElse(null);
    }
}
