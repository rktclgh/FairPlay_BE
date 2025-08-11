package com.fairing.fairplay.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.user.entity.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Users> findByEmailAndName(String email, String name);

    Optional<Users> findByUserId(Long userId);

    @Query("SELECT u FROM Users u WHERE u.roleCode.id <= 2")
    List<Users> findAdmin();

}
