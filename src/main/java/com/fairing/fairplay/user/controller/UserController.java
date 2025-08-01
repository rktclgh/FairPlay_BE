package com.fairing.fairplay.user.controller;

import com.fairing.fairplay.user.dto.*;
import com.fairing.fairplay.user.service.UserService;
import com.fairing.fairplay.core.security.CustomUserDetails; // 실제 위치에 맞게 import
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid UserRegisterRequestDto dto) {
        userService.register(dto);
        return ResponseEntity.ok().build();
    }

    // 내 정보 조회
    @GetMapping("/mypage")
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(userService.getMyInfo(userId));
    }

    // 내 정보 수정
    @PostMapping("/mypage/edit")
    public ResponseEntity<UserResponseDto> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequestDto dto
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(userService.updateMyInfo(userId, dto));
    }

    // 회원 탈퇴
    @PostMapping("/mypage/quit")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        userService.deleteMyAccount(userId);
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 변경
    @PutMapping("/mypage/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserPasswordUpdateRequestDto dto
    ) {
        Long userId = userDetails.getUserId();
        userService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // 임시 비밀번호 전송
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody UserForgotPasswordRequestDto dto) {
        userService.sendTemporaryPassword(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailDuplicate(@RequestParam String email) {
        boolean duplicated = userService.isEmailDuplicated(email);
        Map<String, Boolean> result = new HashMap<>();
        result.put("duplicate", duplicated);
        return ResponseEntity.ok(result);
    }

    // 닉네임 중복 확인
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNameDuplicate(@RequestParam String nickname) {
        boolean duplicated = userService.isNicknameDuplicated(nickname);
        Map<String, Boolean> result = new HashMap<>();
        result.put("duplicate", duplicated);
        return ResponseEntity.ok(result);
    }
}
