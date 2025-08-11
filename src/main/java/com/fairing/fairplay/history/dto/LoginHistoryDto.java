package com.fairing.fairplay.history.dto;

import java.time.LocalDateTime;

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
    private int user_role_code_id;
    private String ip;
    private String userAgent;
    private LocalDateTime loginTime;
}
