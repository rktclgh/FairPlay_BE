package com.fairing.fairplay.user.controller;

import com.fairing.fairplay.user.dto.UserRegisterRequestDto;
import com.fairing.fairplay.user.dto.UserLoginRequestDto;
import com.fairing.fairplay.user.dto.UserResponseDto;
import com.fairing.fairplay.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody @Valid UserRegisterRequestDto dto) {
        userService.register(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@RequestBody @Valid UserLoginRequestDto dto) {
        UserResponseDto user = userService.login(dto);
        return ResponseEntity.ok(user);
    }
}
