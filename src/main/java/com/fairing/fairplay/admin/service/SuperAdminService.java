package com.fairing.fairplay.admin.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

@Service
public class SuperAdminService {
    private final UserRepository userRepository;
    private final LevelService levelService;

    public SuperAdminService(UserRepository userRepository, LevelService levelService) {
        this.userRepository = userRepository;
        this.levelService = levelService;
    }

    public void disableUser(Long userId, Long targetId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void setEventAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(100).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
    }

    public void setSuperAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
    }

    public void setBoothAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(6).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
    }

    public void setAuth(Long userId, BigInteger functionLevel) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        BigInteger accountLevel = levelService.getAccountLevel(userId);
    }

}
