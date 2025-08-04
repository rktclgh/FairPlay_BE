package com.fairing.fairplay.core.email.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class EmailCodeVerifyRequestDto {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String code;
}
