package com.fairing.fairplay.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class BoothAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_admin_id")
    private Long id;

    @Column(name = "manager_name", nullable = false, length = 20)
    private String managerName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "official_url", nullable = false, length = 512)
    private String officialUrl;

}
