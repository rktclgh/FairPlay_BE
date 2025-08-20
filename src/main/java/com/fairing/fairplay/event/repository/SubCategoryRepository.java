package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.MainCategory;
import com.fairing.fairplay.event.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Integer> {
    List<SubCategory> findByMainCategory(MainCategory mainCategory);
}
