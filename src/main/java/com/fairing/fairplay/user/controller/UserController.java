package com.fairing.fairplay.user.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.user.dto.*;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private static final Integer ADMIN = 1;    // 전체 관리자
    private static final Integer EVENT = 2;    // 행사 관리자
    private static final Integer BOOTH = 3;    // 부스 관리자
    private static final Integer COMMON = 4;   // 일반 사용자

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

    // 행사 관리자 정보 확인
    @GetMapping("/event-admin/{eventId}/public")
    public ResponseEntity<EventAdminResponseDto> getEventAdmin(@PathVariable Long eventId) {

        EventAdminResponseDto responseDto = userService.getEventAdminInfo(eventId);

        return ResponseEntity.ok(responseDto);
    }

    // 행사 관리자 정보 수정
    @PatchMapping("/event-admin/{eventId}")
    public ResponseEntity<EventAdminResponseDto> updateEventAdminInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody @Valid EventAdminRequestDto dto) {

        checkAuth(userDetails, EVENT);
        checkEventManager(userDetails, eventId);

        EventAdminResponseDto responseDto = userService.updateEventAdmin(eventId, dto);

        return ResponseEntity.ok(responseDto);
    }

    private void checkAuth(CustomUserDetails userDetails, Integer authority) {
        log.info("기본 권한 확인");

        if (userDetails.getRoleId() > authority) {
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private void checkEventManager(CustomUserDetails userDetails, Long eventId) {
        log.info("행사 관리자 추가 권한 확인");
        Long managerId = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."))
                .getManager().getUserId();

        Integer authority = userDetails.getRoleId();

        if (!authority.equals(ADMIN) && !managerId.equals(userDetails.getUserId())) throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
    }

}
