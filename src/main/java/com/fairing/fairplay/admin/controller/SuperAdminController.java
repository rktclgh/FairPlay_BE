package com.fairing.fairplay.admin.controller;

import java.time.LocalDateTime;
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

import com.fairing.fairplay.admin.service.LevelService;
import com.fairing.fairplay.admin.service.SuperAdminService;
import com.fairing.fairplay.common.exception.CustomException;
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

    @PostMapping("/send-mail/{userId}")
    public ResponseEntity<String> sendAccountMail(
            @PathVariable Long userId) {

        // checkAuth(userDetails, ADMIN);

        /*
         * 행사관리자 신청시 이메일,이름 등의 정보를 입력받을 예정이므로 해당 데이터 사용할 예정
         * 
         */
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        String to = user.getEmail();
        String subject = "FairPlay 관리자 메일";
        superAdminService.sendMail(to, subject);
        return ResponseEntity.ok("메일 발송 완료.");

    }

    @GetMapping("/get-logs")
    public ResponseEntity<List<LoginHistoryDto>> getLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // checkAuth(userDetails, ADMIN);

        List<LoginHistoryDto> loginHistories = loginHistoryService.getAllLoginHistory();
        return ResponseEntity.ok(loginHistories);
    }

    private void checkAuth(CustomUserDetails userDetails, Integer requiredRole) {
        if (userDetails.getRoleId() > requiredRole) {
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    @PostMapping("/disable-user/{userId}")
    public ResponseEntity disableUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // checkAuth(userDetails, ADMIN);
        superAdminService.disableUser(userDetails.getUserId(), userId);
        return ResponseEntity.ok("사용자 비활성화 완료.");
    }

    @GetMapping("/get-users")
    public ResponseEntity<List<Users>> getUsers() {
        List<Users> users = userRepository.findAdmin();
        return ResponseEntity.ok(users);
    }

    @GetMapping("test")
    public Boolean test() {
        Long functionLevel = levelService.getFunctionLevel(getMethodName());

        Long accountLevel = levelService.getAccountLevel(1L);
        return (accountLevel & functionLevel) == functionLevel;
    }

    public String getMethodName() {
        StackWalker walker = StackWalker.getInstance();
        return walker.walk(frames -> frames.skip(1).findFirst().get().getMethodName());
    }

}
