package com.fairing.fairplay.creator.repository;

import com.fairing.fairplay.creator.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, Long> {

    // 활성화된 제작자만 조회 (표시 순서대로)
    List<Creator> findByIsActiveTrueOrderByDisplayOrderAsc();

    // 모든 제작자 조회 (표시 순서대로)
    List<Creator> findAllByOrderByDisplayOrderAsc();
}
