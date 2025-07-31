package com.fairing.fairplay.user.service;

import com.fairing.fairplay.user.dto.UserRegisterRequestDto;
import com.fairing.fairplay.user.dto.UserLoginRequestDto;
import com.fairing.fairplay.user.dto.UserResponseDto;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 회원가입
    public void register(UserRegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        UserRoleCode role = userRoleCodeRepository.findByCode("COMMON")
                .orElseThrow(() -> new IllegalArgumentException("기본 역할코드(COOKIE) 없음"));

        Users user = Users.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .roleCode(role)
                .build();

        userRepository.save(user);
    }



    // 로그인
    public UserResponseDto login(UserLoginRequestDto dto) {
        Users user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRoleCode().getCode())
                .build();
    }
}
