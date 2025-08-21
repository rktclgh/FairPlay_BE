package com.fairing.fairplay.businesscard.repository;

import com.fairing.fairplay.businesscard.entity.BusinessCard;
import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessCardRepository extends JpaRepository<BusinessCard, Long> {
    
    Optional<BusinessCard> findByUser(Users user);
    
    Optional<BusinessCard> findByUserUserId(Long userId);
    
    boolean existsByUser(Users user);
}