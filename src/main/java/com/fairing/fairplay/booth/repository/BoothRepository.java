package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothType;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {
    List<Booth> findAllByEvent(Event event);
    List<Booth> findByEventAndIsDeletedFalse(Event event);
    long countByBoothTypeAndIsDeletedFalse(BoothType boothType);
}