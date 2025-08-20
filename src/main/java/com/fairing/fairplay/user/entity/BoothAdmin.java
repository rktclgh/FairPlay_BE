package com.fairing.fairplay.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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

}