package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomAccessService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;

    public void assertCanAccess(ChatRoom chatRoom, Long userId, String roleCode, String message) {
        if (!canAccess(chatRoom, userId, roleCode)) {
            throw new AccessDeniedException(userId == null
                    ? "인증된 사용자만 채팅방에 접근할 수 있습니다."
                    : message);
        }
    }

    public boolean canAccess(ChatRoom chatRoom, Long userId) {
        return canAccess(chatRoom, userId, null);
    }

    public boolean canAccess(ChatRoom chatRoom, Long userId, String roleCode) {
        if (chatRoom == null || userId == null) {
            return false;
        }

        if (userId.equals(chatRoom.getUserId()) || userId.equals(chatRoom.getTargetId())) {
            return true;
        }

        return TargetType.ADMIN.equals(chatRoom.getTargetType()) && isAdmin(userId, roleCode);
    }

    private boolean isAdmin(Long userId, String roleCode) {
        if (roleCode != null) {
            return ADMIN_ROLE.equals(roleCode);
        }
        return !userRepository.findByUserIdInAndRoleCode_Code(Set.of(userId), ADMIN_ROLE).isEmpty();
    }
}
