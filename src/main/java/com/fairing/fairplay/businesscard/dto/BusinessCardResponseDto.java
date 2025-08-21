package com.fairing.fairplay.businesscard.dto;

import com.fairing.fairplay.businesscard.entity.BusinessCard;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
    private String buildingName;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeUrl;
    private String detailAddress;
    private String description;
    private String linkedIn;
    private String instagram;
    private String facebook;
    private String twitter;
    private String github;
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
                .address(businessCard.getLocation() != null ? businessCard.getLocation().getAddress() : null)
                .buildingName(businessCard.getLocation() != null ? businessCard.getLocation().getBuildingName() : null)
                .placeName(businessCard.getLocation() != null ? businessCard.getLocation().getPlaceName() : null)
                .latitude(businessCard.getLocation() != null ? businessCard.getLocation().getLatitude() : null)
                .longitude(businessCard.getLocation() != null ? businessCard.getLocation().getLongitude() : null)
                .placeUrl(businessCard.getLocation() != null ? businessCard.getLocation().getPlaceUrl() : null)
                .detailAddress(businessCard.getDetailAddress())
                .description(businessCard.getDescription())
                .linkedIn(businessCard.getLinkedIn())
                .instagram(businessCard.getInstagram())
                .facebook(businessCard.getFacebook())
                .twitter(businessCard.getTwitter())
                .github(businessCard.getGithub())
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
                .buildingName(isNotEmpty(this.buildingName) ? this.buildingName : null)
                .placeName(isNotEmpty(this.placeName) ? this.placeName : null)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .placeUrl(isNotEmpty(this.placeUrl) ? this.placeUrl : null)
                .detailAddress(isNotEmpty(this.detailAddress) ? this.detailAddress : null)
                .description(isNotEmpty(this.description) ? this.description : null)
                .linkedIn(isNotEmpty(this.linkedIn) ? this.linkedIn : null)
                .instagram(isNotEmpty(this.instagram) ? this.instagram : null)
                .facebook(isNotEmpty(this.facebook) ? this.facebook : null)
                .twitter(isNotEmpty(this.twitter) ? this.twitter : null)
                .github(isNotEmpty(this.github) ? this.github : null)
                .profileImageUrl(isNotEmpty(this.profileImageUrl) ? this.profileImageUrl : null)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}