package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomAccessService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserRepository userRepository;

    public boolean canAccess(ChatRoom chatRoom, Long userId) {
        if (chatRoom == null || userId == null) {
            return false;
        }
        if (userId.equals(chatRoom.getUserId()) || userId.equals(chatRoom.getTargetId())) {
            return true;
        }
        if (TargetType.ADMIN.equals(chatRoom.getTargetType())) {
            return userRepository.findById(userId)
                    .map(user -> user.getRoleCode())
                    .map(role -> ROLE_ADMIN.equals(role.getCode()))
                    .orElse(false);
        }
        return false;
    }
}
