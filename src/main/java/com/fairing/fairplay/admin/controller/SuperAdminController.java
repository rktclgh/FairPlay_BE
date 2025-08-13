package com.fairing.fairplay.admin.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.admin.dto.AdminAuthDto;
import com.fairing.fairplay.admin.dto.FunctionNameDto;
import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.admin.service.LevelService;
import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.etc.ChangeAccount;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final UserRepository userRepository;
    private final LoginHistoryService loginHistoryService;
    private final LevelService levelService;
    private final FunctionLevelRepository functionLevelRepository;

    public SuperAdminController(SuperAdminService superAdminService, UserRepository userRepository,
            LoginHistoryService loginHistoryService, LevelService levelService,
            FunctionLevelRepository functionLevelRepository) {
        this.superAdminService = superAdminService;
        this.userRepository = userRepository;
        this.loginHistoryService = loginHistoryService;
        this.levelService = levelService;
        this.functionLevelRepository = functionLevelRepository;
    }

    @FunctionAuth("getLogs")
    @GetMapping("/get-logs")
    // 권한 관련 기능 추가예정
    public ResponseEntity<List<LoginHistoryDto>> getLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<LoginHistoryDto> loginHistories = loginHistoryService.getAllLoginHistory();
        return ResponseEntity.ok(loginHistories);
    }

    @ChangeAccount("관리자 비활성화")
    @FunctionAuth("disableUser")
    @PostMapping("/disable-user/{userId}")
    // 권한 관련 기능 추가 예정
    public ResponseEntity<String> disableUser(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long userId) {
        superAdminService.disableUser(userDetails.getUserId(), userId);
        return ResponseEntity.ok("사용자 비활성화 완료.");
    }

    // 관리자 목록(전체관리자/행사관리자/부스관리자) 리턴
    @GetMapping("/get-admins")
    public ResponseEntity<List<AdminAuthDto>> getAdmins(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Users> users = userRepository.findAdmin();

        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        List<AdminAuthDto> adminAuthDtos = new ArrayList<>();

        for (Users user : users) {
            List<String> auths = new ArrayList<>();
            BigInteger accountLevel = levelService.getAccountLevel(user.getUserId());

            for (FunctionLevel functionLevel : functionLevels) {
                if (accountLevel.and(functionLevel.getLevel().toBigInteger())
                        .equals(functionLevel.getLevel().toBigInteger())) {
                    auths.add(functionLevel.getFunctionName());
                }
            }
            AdminAuthDto dto = new AdminAuthDto();
            dto.setUserId(user.getUserId());
            dto.setRole(user.getRoleCode().getName());
            dto.setNickname(user.getNickname());
            dto.setEmail(user.getEmail());
            dto.setAuthList(auths);
            adminAuthDtos.add(dto);
        }

        return ResponseEntity.ok(adminAuthDtos);
    }

    // 권한(영어,한글) 목록 리턴
    // 기존 85+a에서, 10개로 추린 리스트
    // 권한 관련 기능 추가예정
    @GetMapping("/get-auth-list")
    public ResponseEntity<List<FunctionNameDto>> getAuthList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        List<FunctionNameDto> functionNameDtos = functionLevels.stream()
                .map(functionLevel -> {
                    FunctionNameDto dto = new FunctionNameDto();
                    dto.setFunctionName(functionLevel.getFunctionName());
                    dto.setFunctionNameKr(functionLevel.getFunctionNameKr());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(functionNameDtos);
    }

    @ChangeAccount("권한 설정")
    @PostMapping("/modify-auth/{userId}")
    // 권한매핑 추가예정
    public ResponseEntity<String> modifyAuth(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId,
            @RequestBody List<String> authList) {
        superAdminService.modifyAuth(userId, authList);
        return ResponseEntity.ok("권한 수정 완료.");
    }

}
