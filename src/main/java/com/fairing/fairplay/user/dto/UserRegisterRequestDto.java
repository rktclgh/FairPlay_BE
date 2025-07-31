package com.fairing.fairplay.user.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserRegisterRequestDto {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;


    @NotNull
    private Integer roleCodeId;

}
