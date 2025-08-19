package com.fairing.fairplay.admin.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.etc.InitializeLevel;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;

import jakarta.annotation.PostConstruct;

@Service
public class LevelService {
    private final AccountLevelRepository accountLevelRepository;
    private final FunctionLevelRepository functionLevelRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleCodeRepository userRoleCodeRepository;

    public LevelService(AccountLevelRepository accountLevelRepository,
            FunctionLevelRepository functionLevelRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, UserRoleCodeRepository userRoleCodeRepository) {
        this.accountLevelRepository = accountLevelRepository;
        this.functionLevelRepository = functionLevelRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleCodeRepository = userRoleCodeRepository;
    }

    public BigInteger getAccountLevel(Long userId) {

        return accountLevelRepository.findById(userId)
                .map(accountLevel -> accountLevel.getLevel().toBigInteger())
                .orElse(null);
    }

    // 기능별 권한을 주기위해 프로젝트 실행시 functionlevel 테이블의 값을 날리고 초기화 하는과정
    // sql로 값을 추가할 수 있으나, 2^53? 이상의 값에대해 정밀도가 떨어져서 제대로된 값이 들어가지 않기때문에 자바에서 초기화함
    @PostConstruct
    public void initLevel() {
        functionLevelRepository.deleteAll();
        List<FunctionLevel> functionLevels = new InitializeLevel().getFunctionLevels();
        functionLevelRepository.saveAll(functionLevels);
    }

    @PostConstruct
    @Transactional
    public void createDefaultAdmin() {
        if (userRepository.findById(1L).isEmpty()) {
            Users admin = new Users();
            admin.setEmail("admin@fair-play.ink");
            admin.setNickname("admin");
            admin.setName("admin");
            admin.setPhone("010-0000-0000");
            admin.setPassword(passwordEncoder.encode("fairplay"));
            admin.setRoleCode(userRoleCodeRepository.findByCode("ADMIN")
                    .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "기본 역할코드를 찾을 수 없습니다.")));
            Users savedAdmin = userRepository.saveAndFlush(admin);

            // AccountLevel 생성 시 userId를 직접 설정
            AccountLevel accountLevel = new AccountLevel();
            accountLevel.setUserId(savedAdmin.getUserId());
            accountLevel.setUser(savedAdmin);
            accountLevel.setLevel(new BigDecimal("1606938044258990275541962092341162602522202993782792835301375"));
            accountLevelRepository.save(accountLevel);
        }
    }
}
