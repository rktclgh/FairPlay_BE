package com.fairing.fairplay.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory.EmailType;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

@Service
public class SuperAdminService {

    @Autowired
    private EmailServiceFactory emailServiceFactory;

    @Autowired
    private UserRepository userRepository;

    public void sendMail(String email, String name) {
        String tempPassword = generateRandomPassword(10);
        emailServiceFactory.getService(EmailType.QUALIFIER).send(email, name, tempPassword);
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

    public void disableUser(Long userId, Long targetId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

}
