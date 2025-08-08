package com.fairing.fairplay.user.repository;

import com.fairing.fairplay.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<Users> findByEmailAndName(String email, String name);
    Optional<Users> findByUserId(Long userId);

    // ADMIN 권한을 가진 사용자들을 조회하여 첫 번째 사용자 반환
    @Query("SELECT u FROM Users u WHERE u.roleCode.code = :roleCode ORDER BY u.userId ASC")
    List<Users> findByRoleCodeCodeOrderByUserIdAsc(@Param("roleCode") String roleCode);

    // ADMIN 권한을 가진 첫 번째 사용자 조회
    default Optional<Users> findFirstByRoleCodeCode(String roleCode) {
        List<Users> users = findByRoleCodeCodeOrderByUserIdAsc(roleCode);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
}
