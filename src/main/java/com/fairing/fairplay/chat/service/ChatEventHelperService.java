package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatEventHelperService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // eventId로 담당자 userId 리턴 (없으면 첫 번째 ADMIN 사용자)
    public Long getManagerUserId(Long eventId) {
        return eventRepository.findById(eventId)
                .map(e -> e.getManager().getUserId())
                .orElseGet(this::getAdminUserId);
    }

    // eventId로 이벤트 제목 리턴
    public String getEventTitle(Long eventId) {
        return eventRepository.findById(eventId)
                .map(e -> e.getTitleKr())
                .orElse("이벤트 문의");
    }

    // ADMIN 권한을 가진 첫 번째 사용자 ID 리턴
    public Long getAdminUserId() {
        return userRepository.findFirstByRoleCodeCode("ADMIN")
                .map(user -> user.getUserId())
                .orElseThrow(() -> new RuntimeException("ADMIN 권한을 가진 사용자를 찾을 수 없습니다."));
    }
}
