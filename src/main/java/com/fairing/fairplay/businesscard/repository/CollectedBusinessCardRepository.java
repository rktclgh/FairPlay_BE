package com.fairing.fairplay.businesscard.repository;

import com.fairing.fairplay.businesscard.entity.CollectedBusinessCard;
import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectedBusinessCardRepository extends JpaRepository<CollectedBusinessCard, Long> {
    
    List<CollectedBusinessCard> findByCollectorOrderByCollectedAtDesc(Users collector);
    
    Optional<CollectedBusinessCard> findByCollectorAndCardOwner(Users collector, Users cardOwner);
    
    boolean existsByCollectorAndCardOwner(Users collector, Users cardOwner);
    
    @Query("SELECT c FROM CollectedBusinessCard c " +
           "JOIN FETCH c.cardOwner co " +
           "LEFT JOIN FETCH co.businessCard " +
           "WHERE c.collector = :collector " +
           "ORDER BY c.collectedAt DESC")
    List<CollectedBusinessCard> findByCollectorWithBusinessCard(@Param("collector") Users collector);
    
    long countByCollector(Users collector);
}