package com.fairing.fairplay.event.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class updateCommonResponseDto {
    private String message;
    private LocalDateTime updatedAt;
    private Integer version;
}
