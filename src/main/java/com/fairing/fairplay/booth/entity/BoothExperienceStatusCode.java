package com.fairing.fairplay.booth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booth_experience_status_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceStatusCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_code_id")
    private Integer statusCodeId; // 상태 코드 ID (PK)

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code; // 상태 코드 (WAITING, READY, IN_PROGRESS, COMPLETED, CANCELLED)

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 상태명 (대기중, 입장가능, 체험중, 완료, 취소)
}