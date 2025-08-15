package com.fairing.fairplay.history.etc;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.fairing.fairplay.history.entity.LoginHistory;

public class LoginHistorySpec {
    public static Specification<LoginHistory> searchByEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isEmpty())
                return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("user").get("email"), email);
        };
    }

    public static Specification<LoginHistory> searchByTime(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null)
                return criteriaBuilder.conjunction();
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("loginTime"), from, to);
            } else if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("loginTime"), from);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("loginTime"), to);
            }
        };
    }
}
