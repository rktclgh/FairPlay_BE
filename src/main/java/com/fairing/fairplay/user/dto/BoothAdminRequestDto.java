package com.fairing.fairplay.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothAdminRequestDto {
    String managerName;
    String contactNumber;
    String contactEmail;
}
