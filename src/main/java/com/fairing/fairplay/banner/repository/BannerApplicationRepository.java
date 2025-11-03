// BannerApplicationRepository.java
package com.fairing.fairplay.banner.repository;

import com.fairing.fairplay.banner.entity.BannerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BannerApplicationRepository extends JpaRepository<BannerApplication, Long> {

    List<BannerApplication> findAllByOrderByCreatedAtDesc();

    @Query("SELECT ba FROM BannerApplication ba " +
           "LEFT JOIN FETCH ba.event e " +
           "LEFT JOIN FETCH e.manager em " +
           "LEFT JOIN FETCH em.user u " +
           "LEFT JOIN FETCH ba.bannerType bt " +
           "LEFT JOIN FETCH ba.statusCode sc " +
           "LEFT JOIN FETCH ba.payment p " +
           "LEFT JOIN FETCH p.paymentStatusCode psc " +
           "ORDER BY ba.createdAt DESC")
    List<BannerApplication> findAllWithHostInfoOrderByCreatedAtDesc();

    @Query("SELECT ba FROM BannerApplication ba " +
           "LEFT JOIN FETCH ba.bannerType bt " +
           "LEFT JOIN FETCH ba.statusCode sc " +
           "LEFT JOIN FETCH ba.payment p " +
           "LEFT JOIN FETCH p.paymentStatusCode psc " +
           "LEFT JOIN FETCH ba.applicantId a " +
           "WHERE ba.id = :id")
    Optional<BannerApplication> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT ba FROM BannerApplication ba " +
           "LEFT JOIN FETCH ba.event e " +
           "LEFT JOIN FETCH ba.bannerType bt " +
           "LEFT JOIN FETCH ba.statusCode sc " +
           "WHERE ba.applicantId.userId = :userId " +
           "ORDER BY ba.createdAt DESC")
    List<BannerApplication> findByApplicantIdOrderByCreatedAtDesc(@Param("userId") Long userId);

}
