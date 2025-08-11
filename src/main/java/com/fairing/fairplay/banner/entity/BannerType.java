package com.fairing.fairplay.banner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "banner_type")
@Getter
@Setter
@NoArgsConstructor
public class BannerType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_type_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // NEW, MD_PICK, HOT

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer price = 0;

    @Column(length = 255)
    private String description;
}

