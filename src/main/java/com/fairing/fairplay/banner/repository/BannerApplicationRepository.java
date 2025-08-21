// BannerApplicationRepository.java
package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerApplicationRepository extends JpaRepository<BannerApplication, Long> {

    List<BannerApplication> findAllByOrderByCreatedAtDesc();

}
