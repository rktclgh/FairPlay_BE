package com.fairing.fairplay.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserUpdateRequestDto {
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;
    @Size(min = 2, max = 50, message = "닉네임은 2-50자 사이여야 합니다")
    private String nickname;
    // nickname 필요시 추가
}
