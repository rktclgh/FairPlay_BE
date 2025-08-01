package com.fairing.fairplay.user.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.util.EmailSender;
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
    private final EmailSender emailSender;

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
    public void sendTemporaryPassword(String email, String name) {
        Users user = userRepository.findByEmailAndName(email, name)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원이 없습니다."));

        String tempPassword = generateRandomPassword(10);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        String htmlContent = String.format("""
            <!doctype html>
            <html lang="ko">
            <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>이메일 템플릿</title>
    <style>
      body {
        font-family: "Segoe UI", "Apple SD Gothic Neo", Arial, sans-serif;
        background: #f1f5f9;
        margin: 0;
        padding: 20px;
      }
      .container {
        max-width: 400px;
        margin: 0 auto;
        border: 1px solid #e2e8f0;
        border-radius: 16px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
        padding: 40px 24px 32px 24px;
        background: #ffffff;
      }
      .logo {
        text-align: center;
        margin-bottom: 24px;
      }
      .logo-icon {
        width: 64px;
        height: 64px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
      }
      .logo-icon span {
        font-size: 28px;
      }
      h2 {
        color: #1f2937;
        text-align: center;
        font-size: 24px;
        font-weight: 600;
        margin: 0;
      }
      .brand {
        color: #3b82f6;
        font-weight: 700;
      }
      .message-box {
        margin: 24px 0;
        padding: 20px;
        border-radius: 12px;
        background: linear-gradient(135deg, #f8fafc, #f1f5f9);
        border: 1px solid #e2e8f0;
      }
      .message-box p {
        font-size: 15px;
        color: #475569;
        text-align: center;
        margin: 0;
        line-height: 1.6;
      }
      .highlight {
        color: #3b82f6;
      }
      .code-highlight {
        color: #dc2626;
        font-size: 18px;
      }
      .verification-code {
        font-size: 32px;
        font-weight: 700;
        color: #3b82f6;
        letter-spacing: 8px;
        background: #f8fafc;
        border: 2px solid #e2e8f0;
        border-radius: 12px;
        text-align: center;
        margin: 20px 0 24px 0;
        padding: 24px 16px;
        font-family: "Courier New", monospace;
      }
      .warning-box {
        background: #fef2f2;
        border: 1px solid #fecaca;
        border-radius: 8px;
        padding: 16px;
        margin: 20px 0;
      }
      .warning-box ul {
        font-size: 13px;
        color: #4b5563;
        margin: 0;
        line-height: 1.5;
        padding-left: 20px;
      }
      .warning-box li {
        margin-bottom: 6px;
      }
      .warning-highlight {
        color: #dc2626;
      }
      .contact-link {
        color: #3b82f6;
        text-decoration: none;
        font-weight: 500;
      }
      .footer {
        margin-top: 32px;
        text-align: center;
        padding-top: 20px;
        border-top: 1px solid #f1f5f9;
      }
      .footer span {
        font-size: 12px;
        color: #9ca3af;
      }
    </style>
  </head>
                        <body>
                           <div class="container">
                             <div class="logo">
                               <div class="logo-icon">
                                 <img src="cid:logo" alt="FairPlay Logo" style="width: 64px; height: 64px; object-fit: contain"/>
                               </div>
                               <h2><span class="brand">FairPlay</span> 임시 비밀번호</h2>
                             </div>
                             <div class="message-box">
                               <p>
                                 안녕하세요! <strong class="highlight">FairPlay</strong>를 이용해주셔서 감사합니다.<br/>
                                 아래 <strong class="code-highlight">임시 비밀번호</strong>로<br/>
                                 로그인 후 반드시 <strong>비밀번호를 변경</strong>해 주세요.
                               </p>
                             </div>
                             <div class="verification-code">%s</div>
                             <div class="warning-box">
                               <ul>
                                 <li>🔒 <strong>임시 비밀번호는 타인에게 절대 공유하지 마세요</strong></li>
                                 <li>📞 문의: <a href="mailto:support@fair-play.ink" class="contact-link">support@fair-play.ink</a></li>
                               </ul>
                             </div>
                             <div class="footer">
                               <span>© 2025 FairPlay · 박람회/행사 예약 플랫폼</span>
                             </div>
                           </div>
                         </body>
                       </html>
  """, tempPassword, tempPassword);

        String logoCid = "logo";
        String logoPath = "etc/logo.png";

        emailSender.send(
                user.getEmail(),
                "[FairPlay] 임시 비밀번호 안내",
                htmlContent,
                logoCid,
                logoPath
        );
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
