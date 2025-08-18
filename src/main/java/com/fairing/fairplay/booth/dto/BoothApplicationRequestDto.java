package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.file.dto.TempFileUploadDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BoothApplicationRequestDto {

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
    private String contactEmail;

    @NotBlank
    @Email
    @Size(max = 100)
    private String boothEmail;

    @NotBlank
    @Size(max = 20)
    private String contactNumber;

    private List<BoothExternalLinkDto> boothExternalLinks;

    @NotNull
    @Future(message = "시작일은 현재보다 이후여야 합니다.")
    private LocalDate startDate;

    @NotNull
    @Future(message = "종료일은 현재보다 이후여야 합니다.")
    private LocalDate endDate;

    private TempFileUploadDto tempBannerUrl;

}
