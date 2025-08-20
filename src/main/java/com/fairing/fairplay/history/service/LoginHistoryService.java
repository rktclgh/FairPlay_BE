package com.fairing.fairplay.history.service;

import org.springframework.stereotype.Service;

import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.entity.LoginHistory;
import com.fairing.fairplay.history.repository.LoginHistoryRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;

@Service
public class LoginHistoryService {

        private final LoginHistoryRepository loginHistoryRepository;

        private final UserRepository userRepository;

        private final UserRoleCodeRepository userRoleCodeRepository;

        public LoginHistoryService(LoginHistoryRepository loginHistoryRepository,
                        UserRepository userRepository,
                        UserRoleCodeRepository userRoleCodeRepository) {
                this.loginHistoryRepository = loginHistoryRepository;
                this.userRepository = userRepository;
                this.userRoleCodeRepository = userRoleCodeRepository;
        }

        public void saveLoginHistory(LoginHistoryDto loginHistory) {
                Users user = userRepository.findById(loginHistory.getUserId())
                                .orElseThrow();

                LoginHistory loginHistoryEntity = LoginHistory.builder()
                                .user(user)
                                .roleCode(userRoleCodeRepository.findById(loginHistory.getUser_role_code_id())
                                                .orElseThrow())
                                .ip(loginHistory.getIp())
                                .loginTime(loginHistory.getLoginTime())
                                .userAgent(loginHistory.getUserAgent())
                                .build();

                loginHistoryRepository.save(loginHistoryEntity);
        }

        // public List<LoginHistoryDto> getAllLoginHistory() {
        // List<LoginHistory> histories =
        // loginHistoryRepository.findAllByOrderByLoginTimeDesc();
        // return histories.stream()
        // .map(history -> new LoginHistoryDto(
        // history.getId(),
        // history.getUser().getUserId(),
        // history.getUser().getName(),
        // history.getUser().getEmail(),
        // history.getRoleCode().getId(),
        // history.getIp(),
        // history.getUserAgent(),
        // history.getLoginTime()))
        // .toList();
        // }

}