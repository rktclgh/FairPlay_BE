package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothExternalLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothExternalLinkRepository extends JpaRepository<BoothExternalLink, Long> {
    List<BoothExternalLink> findByBooth(Booth booth);

    void deleteByBooth(Booth booth);
}
