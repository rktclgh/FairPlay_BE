package com.fairing.fairplay.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
<<<<<<< HEAD
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.user.entity.Users;
=======
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
>>>>>>> b54f95211f4b458ad32afdb371f89f84fa31bc28

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Users> findByEmailAndName(String email, String name);

    Optional<Users> findByUserId(Long userId);

<<<<<<< HEAD
    @Query("SELECT u FROM Users u WHERE u.roleCode.id <= 2")
    List<Users> findAdmin();
=======
    // ADMIN 권한을 가진 사용자들을 조회하여 첫 번째 사용자 반환
    @Query("SELECT u FROM Users u WHERE u.roleCode.code = :roleCode ORDER BY u.userId ASC")
    List<Users> findByRoleCodeCodeOrderByUserIdAsc(@Param("roleCode") String roleCode);
>>>>>>> b54f95211f4b458ad32afdb371f89f84fa31bc28

    // ADMIN 권한을 가진 첫 번째 사용자 조회
    default Optional<Users> findFirstByRoleCodeCode(String roleCode) {
        List<Users> users = findByRoleCodeCodeOrderByUserIdAsc(roleCode);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    // 특정 사용자 ID들 중에서 특정 권한을 가진 사용자들 조회
    @Query("SELECT u FROM Users u WHERE u.userId IN :userIds AND u.roleCode.code = :roleCode")
    List<Users> findByUserIdInAndRoleCode_Code(@Param("userIds") Set<Long> userIds, @Param("roleCode") String roleCode);
}
