package com.fairing.fairplay.creator.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorResponseDto {
    private Long id;
    private String name;
    private String email;
    private String profileImage;
    private String role;
    private String bio;
    private List<String> responsibilities;
    private String github;
    private String linkedin;
    private String instagram;
    private String twitter;
    private String website;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
