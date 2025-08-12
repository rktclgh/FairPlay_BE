package com.fairing.fairplay.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    public enum Role { SYSTEM, USER, ASSISTANT }
    private Role role;
    private String content;
    
    public static ChatMessageDto system(String content) {
        return ChatMessageDto.builder().role(Role.SYSTEM).content(content).build();
    }
    
    public static ChatMessageDto user(String content) {
        return ChatMessageDto.builder().role(Role.USER).content(content).build();
    }
    
    public static ChatMessageDto assistant(String content) {
        return ChatMessageDto.builder().role(Role.ASSISTANT).content(content).build();
    }
}
