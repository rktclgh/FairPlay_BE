package com.fairing.fairplay.settlement.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccountRequestDto {
    @NotBlank(message = "은행명은 필수 입력값입니다.")
    @Size(min = 2, message = "은행명은 최소 2자 이상이어야 합니다.")
    private String bankName;

    @NotBlank(message = "계좌번호는 필수 입력값입니다.")
    @Pattern(regexp = "\\d{10,14}", message = "계좌번호는 10자리 이상 14자리 이하 숫자여야 합니다.")
    private String accountNumber;

    @NotBlank(message = "예금주명은 필수 입력값입니다.")
    @Pattern(regexp = "^[a-zA-Z가-힣\\s]+$", message = "예금주명은 한글 또는 영문만 입력 가능합니다.")
    private String holderName;
}
