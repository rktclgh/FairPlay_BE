package com.fairing.fairplay.businesscard.entity;

import com.fairing.fairplay.event.entity.Location;
import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_cards")
@Data
@EntityListeners(AuditingEntityListener.class)
public class BusinessCard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cardId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;
    
    @Column(length = 100)
    private String name;
    
    @Column(length = 100)
    private String company;
    
    @Column(length = 100)
    private String position;
    
    @Column(length = 100)
    private String department;
    
    @Column(length = 20)
    private String phoneNumber;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 200)
    private String website;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
    
    @Column(length = 300)
    private String detailAddress;
    
    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String linkedIn;
    
    @Column(length = 200)
    private String instagram;
    
    @Column(length = 200)
    private String facebook;
    
    @Column(length = 200)
    private String twitter;

    @Column(length = 200)
    private String github;

    @Column(length = 300)
    private String profileImageUrl;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}