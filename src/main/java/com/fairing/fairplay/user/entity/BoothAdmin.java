package com.fairing.fairplay.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fairing.fairplay.user.entity.Users;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "booth_admin")
public class BoothAdmin {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "manager_name", nullable = false, length = 20)
    private String managerName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "official_url", nullable = false, length = 512)
    private String officialUrl;

}