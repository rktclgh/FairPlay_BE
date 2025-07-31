package com.fairing.fairplay.user.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserLoginRequestDto {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}