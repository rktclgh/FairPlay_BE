package com.fairing.fairplay.businesscard.dto;

import com.fairing.fairplay.businesscard.entity.BusinessCard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BusinessCardResponseDto {
    private Long cardId;
    private Long userId;
    private String name;
    private String company;
    private String position;
    private String department;
    private String phoneNumber;
    private String email;
    private String website;
    private String address;
    private String description;
    private String linkedIn;
    private String instagram;
    private String facebook;
    private String twitter;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static BusinessCardResponseDto from(BusinessCard businessCard) {
        if (businessCard == null) {
            return null;
        }
        
        return BusinessCardResponseDto.builder()
                .cardId(businessCard.getCardId())
                .userId(businessCard.getUser().getUserId())
                .name(businessCard.getName())
                .company(businessCard.getCompany())
                .position(businessCard.getPosition())
                .department(businessCard.getDepartment())
                .phoneNumber(businessCard.getPhoneNumber())
                .email(businessCard.getEmail())
                .website(businessCard.getWebsite())
                .address(businessCard.getAddress())
                .description(businessCard.getDescription())
                .linkedIn(businessCard.getLinkedIn())
                .instagram(businessCard.getInstagram())
                .facebook(businessCard.getFacebook())
                .twitter(businessCard.getTwitter())
                .profileImageUrl(businessCard.getProfileImageUrl())
                .createdAt(businessCard.getCreatedAt())
                .updatedAt(businessCard.getUpdatedAt())
                .build();
    }
    
    // 공개용 버전 - 비어있는 필드 제외
    public BusinessCardResponseDto filterEmptyFields() {
        return BusinessCardResponseDto.builder()
                .cardId(this.cardId)
                .userId(this.userId)
                .name(isNotEmpty(this.name) ? this.name : null)
                .company(isNotEmpty(this.company) ? this.company : null)
                .position(isNotEmpty(this.position) ? this.position : null)
                .department(isNotEmpty(this.department) ? this.department : null)
                .phoneNumber(isNotEmpty(this.phoneNumber) ? this.phoneNumber : null)
                .email(isNotEmpty(this.email) ? this.email : null)
                .website(isNotEmpty(this.website) ? this.website : null)
                .address(isNotEmpty(this.address) ? this.address : null)
                .description(isNotEmpty(this.description) ? this.description : null)
                .linkedIn(isNotEmpty(this.linkedIn) ? this.linkedIn : null)
                .instagram(isNotEmpty(this.instagram) ? this.instagram : null)
                .facebook(isNotEmpty(this.facebook) ? this.facebook : null)
                .twitter(isNotEmpty(this.twitter) ? this.twitter : null)
                .profileImageUrl(isNotEmpty(this.profileImageUrl) ? this.profileImageUrl : null)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}