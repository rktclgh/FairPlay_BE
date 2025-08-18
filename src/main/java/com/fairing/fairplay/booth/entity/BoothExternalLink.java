package com.fairing.fairplay.booth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booth_external_link")
public class BoothExternalLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id")
    private Booth booth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_application_id")
    private BoothApplication boothApplication;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "display_text", length = 50)
    private String displayText;
}
