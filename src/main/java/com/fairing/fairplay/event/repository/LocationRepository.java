package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Location findByPlaceName (String placeName);
}
