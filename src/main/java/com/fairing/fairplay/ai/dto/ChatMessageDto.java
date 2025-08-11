package com.fairing.fairplay.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    public enum Role { SYSTEM, USER, ASSISTANT }
    private Role role;
    private String content;
}
