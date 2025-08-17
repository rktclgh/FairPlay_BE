package com.fairing.fairplay.booth.entity;

import com.fairing.fairplay.event.entity.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BoothType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_type_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String size;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "max_applicants")
    private Integer maxApplicants;

    @Column(name = "current_applicants", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer currentApplicants = 0;

}
