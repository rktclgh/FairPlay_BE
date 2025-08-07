package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatEventHelperService {
    private final EventRepository eventRepository;

    // eventId로 담당자 userId 리턴 (없으면 전체 관리자=1L)
    public Long getManagerUserId(Long eventId) {
        return eventRepository.findById(eventId)
                .map(e -> e.getManager().getUserId())
                .orElse(1L);
    }

    // eventId로 이벤트 제목 리턴
    public String getEventTitle(Long eventId) {
        return eventRepository.findById(eventId)
                .map(e -> e.getTitleKr())
                .orElse("이벤트 문의");
    }
}
