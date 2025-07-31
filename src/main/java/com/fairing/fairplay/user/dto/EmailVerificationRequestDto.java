package com.fairing.fairplay.user.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class EmailVerificationRequestDto {
    @NotBlank @Email
    private String email;
}
