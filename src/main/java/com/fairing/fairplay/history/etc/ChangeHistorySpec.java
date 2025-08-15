package com.fairing.fairplay.history.etc;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.fairing.fairplay.history.entity.ChangeHistory;

public class ChangeHistorySpec {
    public static Specification<ChangeHistory> searchByEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isEmpty())
                return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("user").get("email"), email);
        };
    }

    public static Specification<ChangeHistory> searchByTime(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null)
                return criteriaBuilder.conjunction();
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("modifyTime"), from, to);
            } else if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("modifyTime"), from);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("modifyTime"), to);
            }
        };
    }

    public static Specification<ChangeHistory> searchByTargetType(String targetType) {
        return (root, query, criteriaBuilder) -> {
            if (targetType == null || targetType.isEmpty())
                return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("targetType"), targetType);
        };
    }
}
