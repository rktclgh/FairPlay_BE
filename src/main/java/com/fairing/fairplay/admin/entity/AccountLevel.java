package com.fairing.fairplay.admin.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fairing.fairplay.user.entity.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "account_level")
public class AccountLevel {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private Users user;

    private BigDecimal level;

}
