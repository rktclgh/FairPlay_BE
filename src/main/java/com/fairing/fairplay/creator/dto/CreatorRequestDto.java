package com.fairing.fairplay.creator.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorRequestDto {
    private String name;
    private String email;
    private String profileImageUrl;
    private String role;
    private String bio;
    private List<String> responsibilities;
    private String githubUrl;
    private String linkedinUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String websiteUrl;
    private Integer displayOrder;
    private Boolean isActive;
}
