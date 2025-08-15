package com.fairing.fairplay.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fairing.fairplay.user.entity.Users;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_admin")
public class EventAdmin {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(nullable = false, length = 20)
    private String businessNumber;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    @Column(nullable = false, length = 100)
    private String contactEmail;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = false)
    private Boolean verified = false;
}
