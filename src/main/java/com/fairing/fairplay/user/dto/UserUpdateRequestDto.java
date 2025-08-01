package com.fairing.fairplay.user.dto;

import lombok.Getter;

@Getter
public class UserUpdateRequestDto {
    private String phone;
    private String nickname;
    // nickname 필요시 추가
}
