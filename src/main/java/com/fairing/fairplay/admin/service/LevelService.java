package com.fairing.fairplay.admin.service;

import java.math.BigInteger;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.etc.Initialize_level;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;

import jakarta.annotation.PostConstruct;

@Service
public class LevelService {
    private final AccountLevelRepository accountLevelRepository;
    private final FunctionLevelRepository functionLevelRepository;

    public LevelService(AccountLevelRepository accountLevelRepository,
            FunctionLevelRepository functionLevelRepository) {
        this.accountLevelRepository = accountLevelRepository;
        this.functionLevelRepository = functionLevelRepository;
    }

    public BigInteger getAccountLevel(Long userId) {

        return accountLevelRepository.findById(userId)
                .map(accountLevel -> accountLevel.getLevel().toBigInteger())
                .orElse(null);
    }

    // 기능별 권한을 주기위해 프로젝트 실행시 functionlevel 테이블의 값을 날리고 초기화 하는과정
    // sql로 값을 추가할 수 있으나, 2^53? 이상의 값에대해 정밀도가 떨어져서 제대로된 값이 들어가지 않기때문에 자바에서 초기화함
    @PostConstruct
    public void init_level() {
        functionLevelRepository.deleteAll();
        List<FunctionLevel> functionLevels = new Initialize_level().getFunctionLevels();
        functionLevelRepository.saveAll(functionLevels);
    }

}
