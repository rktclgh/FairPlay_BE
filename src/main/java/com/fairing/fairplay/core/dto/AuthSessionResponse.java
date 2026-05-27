package com.fairing.fairplay.core.dto;

import com.fairing.fairplay.user.dto.UserResponseDto;

public record AuthSessionResponse(
        boolean authenticated,
        UserResponseDto user
) {
    public static AuthSessionResponse anonymous() {
        return new AuthSessionResponse(false, null);
    }

    public static AuthSessionResponse authenticated(UserResponseDto user) {
        return new AuthSessionResponse(true, user);
    }
}
