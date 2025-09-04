package com.fairing.fairplay.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String role;
    private Long roleId;
}
