package com.fairing.fairplay.booth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Getter
@Setter
public class BoothApplicationRequestDto {

    @NotNull
    private Long eventId;

    @NotNull
    private Long boothTypeId;

    @NotBlank
    @Size(max = 100)
    private String boothTitle;

    @Size(max = 1000)
    private String boothDescription;

    @NotBlank
    @Size(max = 20)
    private String managerName;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9\\-\\s]+$", message = "숫자, 하이픈(-), 공백만 허용됩니다.")
    @Size(max = 20)
    private String contactNumber;

    @NotBlank
    @URL
    @Size(max = 512)
    private String officialUrl;

    @NotNull
    @Future(message = "시작일은 현재보다 이후여야 합니다.")
    private LocalDate startDate;

    @NotNull
    @Future(message = "종료일은 현재보다 이후여야 합니다.")
    private LocalDate endDate;

}
