package com.fairing.fairplay.admin.dto;

import java.util.List;

import lombok.Data;

@Data
public class AdminAuthDto {
    private Long userId;
    private String role;
    private String nickname;
    private String email;
    private List<String> authList;

}
