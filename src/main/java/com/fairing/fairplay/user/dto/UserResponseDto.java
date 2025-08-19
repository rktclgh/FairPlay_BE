package com.fairing.fairplay.user.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private Long userId;
    private String email;
    private String phone;
    private String name;
    private String nickname;
    private String role; // code 값 (ex: COMMON)
    private LocalDate birthday;
    private String gender;
}