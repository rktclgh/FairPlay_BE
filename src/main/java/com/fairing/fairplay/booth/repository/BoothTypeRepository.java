package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothType;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoothTypeRepository extends JpaRepository<BoothType, Long> {
    Optional<BoothType> findById(Long id);

    List<BoothType> findAllByEvent(Event event);

    @Query("SELECT CASE WHEN bt.currentApplicants < bt.maxApplicants THEN true ELSE false END " +
            "FROM BoothType bt WHERE bt.id = :typeId")
    boolean isAvailable(@Param("typeId") Long typeId);

    @Modifying
    @Query("UPDATE BoothType bt " +
            "SET bt.currentApplicants = bt.currentApplicants + 1 " +
            "WHERE bt.id = :typeId AND bt.currentApplicants < bt.maxApplicants")
    int increaseIfNotFull(@Param("typeId") Long typeId);
}
