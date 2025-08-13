package com.fairing.fairplay.admin.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.admin.dto.AdminAuthDto;
import com.fairing.fairplay.admin.dto.FunctionNameDto;
import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.history.dto.ChangeHistoryDto;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.etc.ChangeAccount;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;

    }

    @FunctionAuth("getLogs")
    @GetMapping("/get-login-logs")
    // 권한 관련 기능 추가예정
    public ResponseEntity<Page<LoginHistoryDto>> getLoginLogs(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<LoginHistoryDto> loginHistories = superAdminService.getLoginLogs(page, size, email, from, to);

        return ResponseEntity.ok(loginHistories);

    }

    @GetMapping("/get-change-logs")
    public ResponseEntity<Page<ChangeHistoryDto>> getChangeLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String email,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<ChangeHistoryDto> changeHistories = superAdminService.getChangeLogs(page, size, email, type, from, to);

        return ResponseEntity.ok(changeHistories);
    }

    @ChangeAccount("관리자 비활성화")
    @FunctionAuth("disableUser")
    @PostMapping("/disable-user/{userId}")
    // 권한 관련 기능 추가 예정
    public ResponseEntity<String> disableUser(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long userId) {

        superAdminService.disableUser(userId);

        return ResponseEntity.ok("사용자 비활성화 완료.");
    }

    // 관리자 목록(전체관리자/행사관리자/부스관리자) 리턴
    @GetMapping("/get-admins")
    public ResponseEntity<List<AdminAuthDto>> getAdmins(@AuthenticationPrincipal CustomUserDetails userDetails) {

        List<AdminAuthDto> adminAuthDtos = superAdminService.getAdmins();

        return ResponseEntity.ok(adminAuthDtos);
    }

    // 권한(영어,한글) 목록 리턴
    // 기존 85+a에서, 10개로 추린 리스트
    // 권한 관련 기능 추가예정
    @GetMapping("/get-auth-list")
    public ResponseEntity<List<FunctionNameDto>> getAuthList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<FunctionNameDto> functionNameDtos = superAdminService.getAuthList();

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
