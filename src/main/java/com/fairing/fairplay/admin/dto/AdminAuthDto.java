package com.fairing.fairplay.admin.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class AdminAuthDto {
    private Long userId;
    private String role;
    private String nickname;
    @ToString.Exclude
    private String email;
    private List<String> authList;

}
