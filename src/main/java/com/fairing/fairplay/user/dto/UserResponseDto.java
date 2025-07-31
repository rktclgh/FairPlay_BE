package com.fairing.fairplay.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String role; // code 값 (ex: COMMON)
}