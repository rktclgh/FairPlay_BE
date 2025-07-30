package com.fairing.fairplay.booth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class BoothType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_type_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String size;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "max_applicants")
    private Integer maxApplicants;

}
