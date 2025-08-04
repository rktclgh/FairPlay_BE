package com.fairing.fairplay.user.dto;

import lombok.Getter;

@Getter
public class UserPasswordUpdateRequestDto {
    private String currentPassword;
    private String newPassword;
}
