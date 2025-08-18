package com.fairing.fairplay.event.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventManagerRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventManagerService {

    private final EventManagerRepository eventManagerRepository;
    private final UserRepository userRepository;

    /**
     * 사용자가 관리하는 이벤트 ID 목록 조회
     * EVENT_MANAGER 권한을 가진 사용자가 관리할 수 있는 이벤트들
     */
    @Transactional(readOnly = true)
    public List<Long> getManagedEventIds(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // EVENT_MANAGER 권한 확인
        if (!"EVENT_MANAGER".equals(user.getRoleCode().getCode())) {
            throw new IllegalArgumentException("EVENT_MANAGER 권한이 없습니다.");
        }
        
        // 현재는 사용자가 등록한 모든 이벤트를 관리한다고 가정
        // 추후 event_manager 매핑 테이블이 있다면 해당 테이블에서 조회
        List<Event> managedEvents = eventManagerRepository.findByManager_UserId(userId);
        
        return managedEvents.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 관리하는 이벤트 정보 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Event> getManagedEvents(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        if (!"EVENT_MANAGER".equals(user.getRoleCode().getCode())) {
            throw new IllegalArgumentException("EVENT_MANAGER 권한이 없습니다.");
        }
        
        return eventManagerRepository.findByManager_UserId(userId);
    }

    /**
     * 특정 환불이 해당 관리자의 이벤트 소속인지 검증
     */
    @Transactional(readOnly = true)
    public boolean isRefundInManagedEvent(Long refundId, Long managerId) {
        List<Long> managedEventIds = getManagedEventIds(managerId);
        
        // 환불의 이벤트 ID를 조회하는 쿼리 필요
        // 현재는 간단한 구현으로 대체
        return true; // TODO: 실제 검증 로직 구현
    }

    /**
     * 특정 이벤트가 해당 관리자가 관리하는지 검증
     */
    @Transactional(readOnly = true)
    public boolean canManageEvent(Long eventId, Long managerId) {
        List<Long> managedEventIds = getManagedEventIds(managerId);
        return managedEventIds.contains(eventId);
    }

    /**
     * 관리자가 관리할 수 있는 결제 대상 타입 필터링
     * EVENT_MANAGER는 RESERVATION(티켓), BOOTH(부스)만 관리 가능
     */
    public List<String> getAllowedPaymentTargetTypes() {
        return List.of("RESERVATION", "BOOTH");
    }
}