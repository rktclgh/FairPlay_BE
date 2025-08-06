package com.fairing.fairplay.user.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAdminRequestDto {

    String contactNumber;

    @Email
    String contactEmail;
}
