package com.fairing.fairplay.core.security;

import com.fairing.fairplay.user.entity.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String email;
    private final String name;
    private final String phone;
    private final String roleCode;   // ex: "ADMIN", "COMMON"
    private final String roleName;   // ex: "전체 관리자" (한글)
    private final LocalDateTime deletedAt;

    public CustomUserDetails(Users user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.phone = user.getPhone();
        // roleCode가 null이면 익셉션 방지. (실제로는 null 아니어야 정상)
        this.roleCode = user.getRoleCode() != null ? user.getRoleCode().getCode() : null;
        this.roleName = user.getRoleCode() != null ? user.getRoleCode().getName() : null;
        this.deletedAt = user.getDeletedAt();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한 없으면 빈 Set 반환 (실제론 roleCode가 null이 나오면 DB/로직 점검 필요)
        if (roleCode == null) return Collections.emptySet();
        return Collections.singleton(() -> "ROLE_" + roleCode);
    }

    @Override public String getPassword() { return null; } // 필요시 user.getPassword()
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return deletedAt == null; }
}
