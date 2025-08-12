package com.fairing.fairplay.admin.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.admin.service.LevelService;
import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.aspect.FunctionAuthAspect;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private static final Integer ADMIN = 1; // 전체 관리자
    private static final Integer EVENT = 2; // 행사 관리자
    private static final Integer BOOTH = 3; // 부스 관리자
    private static final Integer COMMON = 4; // 일반 사용자

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @Autowired
    private LevelService levelService;

    @Autowired
    private FunctionLevelRepository functionLevelRepository;

    @FunctionAuth("getLogs")
    @GetMapping("/get-logs")
    public ResponseEntity<List<LoginHistoryDto>> getLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // checkAuth(userDetails, ADMIN);

        List<LoginHistoryDto> loginHistories = loginHistoryService.getAllLoginHistory();
        return ResponseEntity.ok(loginHistories);
    }

    @FunctionAuth("disableUser")
    @PostMapping("/disable-user/{userId}")
    public ResponseEntity disableUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // checkAuth(userDetails, ADMIN);
        superAdminService.disableUser(userDetails.getUserId(), userId);
        return ResponseEntity.ok("사용자 비활성화 완료.");
    }

    @FunctionAuth("getUsers")
    @GetMapping("/get-users")
    public ResponseEntity<List<Users>> getUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // JWT 토큰에서 사용자 정보를 가져와 권한 체크
        checkAuth(userDetails, ADMIN);
        
        List<Users> users = userRepository.findAdmin();
        // checkFunctionAuth(1L);

        return ResponseEntity.ok(users);
    }

}
