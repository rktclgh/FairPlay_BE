package com.fairing.fairplay.businesscard.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BusinessCardRequestDto {
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
}