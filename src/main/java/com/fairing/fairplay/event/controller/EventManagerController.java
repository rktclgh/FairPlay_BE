package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/events/manager")
@RequiredArgsConstructor
@Slf4j
public class EventManagerController {

    private final EventManagerRepository eventManagerRepository;
    
    // 권한 코드 상수
    private static final String EVENT_MANAGER_ROLE = "EVENT_MANAGER";

    /**
     * EVENT_MANAGER가 담당하는 행사의 eventId 조회
     * - 삭제되지 않은 행사 중 가장 최신(eventId가 가장 큰) 1개만 반환
     */
    @GetMapping("/event")
    public ResponseEntity<Long> getMyManagedEvent(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("담당 행사 조회 요청 - 사용자 ID: {}, 권한: {}", 
                 userDetails.getUserId(), userDetails.getRoleCode());
        
        // EVENT_MANAGER 권한 확인
        checkEventManagerRole(userDetails);
        
        Long userId = userDetails.getUserId();
        
        // 담당 행사 조회 (삭제되지 않은 것 중 eventId가 가장 큰 1개)
        Optional<Event> managedEvent = eventManagerRepository.findFirstByManager_UserIdAndIsDeletedFalseOrderByEventIdDesc(userId);
        
        if (managedEvent.isPresent()) {
            Long eventId = managedEvent.get().getEventId();
            log.info("담당 행사 조회 성공 - 사용자 ID: {}, 담당 행사 ID: {}", userId, eventId);
            return ResponseEntity.ok(eventId);
        } else {
            log.info("담당 행사 없음 - 사용자 ID: {}", userId);
            return ResponseEntity.ok(null);
        }
    }

    /**
     * EVENT_MANAGER 권한 확인 (ADMIN 권한 제외)
     */
    private void checkEventManagerRole(CustomUserDetails userDetails) {
        String userRole = userDetails.getRoleCode();
        
        if (!EVENT_MANAGER_ROLE.equals(userRole)) {
            log.warn("권한 없는 접근 시도 - 사용자 ID: {}, 권한: {}", 
                     userDetails.getUserId(), userRole);
            throw new CustomException(HttpStatus.FORBIDDEN, "행사 담당자만 접근할 수 있습니다.");
        }
    }
}