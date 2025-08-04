package com.fairing.fairplay.user.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory;
import com.fairing.fairplay.user.dto.UserRegisterRequestDto;
import com.fairing.fairplay.user.dto.UserResponseDto;
import com.fairing.fairplay.user.dto.UserUpdateRequestDto;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceFactory emailServiceFactory; // 변경!

    // 회원가입
    @Transactional
    public void register(UserRegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
        UserRoleCode role = userRoleCodeRepository.findByCode("COMMON")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "기본 역할코드를 찾을 수 없습니다."));

        Users user = Users.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .roleCode(role)
                .phone(dto.getPhone())
                .nickname(dto.getNickname())
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .role(user.getRoleCode().getCode())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public UserResponseDto updateMyInfo(Long userId, UserUpdateRequestDto dto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        userRepository.save(user);
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .role(user.getRoleCode().getCode())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public void deleteMyAccount(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void sendTemporaryPassword(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원이 없습니다."));

        String tempPassword = generateRandomPassword(10);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 책임 분리: email 모듈에서 임시비번 발송
        emailServiceFactory.getService(EmailServiceFactory.EmailType.TEMPORARY_PASSWORD)
                .send(user.getEmail(), tempPassword);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameDuplicated(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
