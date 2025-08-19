package com.fairing.fairplay.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothAdminResponseDto {
    Long boothId;
    String businessNumber;
    String contactNumber;
    String contactEmail;
}
