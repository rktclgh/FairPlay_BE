package com.fairing.fairplay.history.dto;

import java.time.LocalDateTime;

import com.fairing.fairplay.history.entity.LoginHistory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginHistoryDto {
    private Long id;
    private Long userId;
    private String name;
    private int user_role_code_id;
    private String ip;
    private String userAgent;
    private LocalDateTime loginTime;

    public LoginHistoryDto(LoginHistory loginHistory) {
        this.id = loginHistory.getId();
        this.userId = loginHistory.getUser().getUserId();
        this.name = loginHistory.getUser().getName();
        this.user_role_code_id = loginHistory.getUser().getRoleCode().getId();
        this.ip = loginHistory.getIp();
        this.userAgent = loginHistory.getUserAgent();
        this.loginTime = loginHistory.getLoginTime();
    }
}
