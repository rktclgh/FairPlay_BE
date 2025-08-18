package com.fairing.fairplay.settlement.entitiy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false,unique = true)
    private Settlement settlement; // 정산과 1:1 관계

    @Column(nullable = false, length = 100)
    @NotBlank(message = "은행명은 필수 입력값입니다.")
    @Size(min = 2, message = "은행명은 최소 2자 이상이어야 합니다.")
    private String bankName; // 은행명

    @Column(nullable = false, length = 50)
    @NotBlank(message = "계좌번호는 필수 입력값입니다.")
    @Pattern(regexp = "\\d{10,14}", message = "계좌번호는 10자리 이상 14자리 이하 숫자여야 합니다.")
    private String accountNumber; // 계좌번호

    @Column(nullable = false, length = 100)
    @NotBlank(message = "예금주명은 필수 입력값입니다.")
    @Pattern(regexp = "^[a-zA-Z가-힣\\s]+$", message = "예금주명은 한글 또는 영문만 입력 가능합니다.")
    private String holderName; // 예금주명

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
