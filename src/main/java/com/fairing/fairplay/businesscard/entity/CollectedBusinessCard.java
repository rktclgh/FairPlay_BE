package com.fairing.fairplay.businesscard.entity;

import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "collected_business_cards", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"collector_id", "card_owner_id"}))
@Data
@EntityListeners(AuditingEntityListener.class)
public class CollectedBusinessCard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private Users collector; // 명함을 수집한 사용자
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_owner_id", nullable = false)
    private Users cardOwner; // 명함의 소유자
    
    @Column(length = 100)
    private String memo; // 개인 메모
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime collectedAt;
}