package com.fairing.fairplay.admin.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionLevelEnum;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

@Service
public class SuperAdminService {

    private static final String USER_NOT_FOUND_MSG = "사용자를 찾을 수 없습니다.";

    private final UserRepository userRepository;
    private final AccountLevelRepository accountLevelRepository;
    private final FunctionLevelRepository functionLevelRepository;

    public SuperAdminService(UserRepository userRepository,
            AccountLevelRepository accountLevelRepository, FunctionLevelRepository functionLevelRepository) {
        this.userRepository = userRepository;
        this.accountLevelRepository = accountLevelRepository;
        this.functionLevelRepository = functionLevelRepository;
    }

    public void disableUser(Long userId, Long targetId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void setEventAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(100).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);

    }

    public void setSuperAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);
    }

    public void setBoothAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(6).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);
    }

    public void modifyAuth(Long userId, List<String> authList) {
        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        BigInteger originValue = BigInteger.ZERO;
        for (FunctionLevel functionLevel : functionLevels) {
            originValue = originValue.add(functionLevel.getLevel().toBigInteger());
        }
        BigInteger accountLevel = accountLevelRepository.findByUserId(userId).getLevel().toBigInteger();
        accountLevel = accountLevel.andNot(originValue);
        for (String auth : authList) {
            accountLevel = accountLevel.or(FunctionLevelEnum.fromFunctionName(auth).getBit());
        }
        AccountLevel accountLevelEntity = accountLevelRepository.findByUserId(userId);
        accountLevelEntity.setLevel(new BigDecimal(accountLevel));
        accountLevelRepository.save(accountLevelEntity);
    }

    // public void setAuth(Long userId, BigInteger functionLevel) {
    // Users user = userRepository.findById(userId)
    // .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
    // USER_NOT_FOUND_MSG));
    // BigInteger accountLevel = levelService.getAccountLevel(userId);
    // }

}
