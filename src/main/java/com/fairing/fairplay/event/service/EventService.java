package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.dto.EventRequestDto;
import com.fairing.fairplay.event.dto.EventResponseDto;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventStatusCode;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.EventStatusCodeRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.event.entity.EventVersion;
import com.fairing.fairplay.event.service.EventVersionService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final Hashids hashids;
    private final EventRepository eventRepository;
    private final EventStatusCodeRepository eventStatusCodeRepository;
    private final EventAdminRepository eventAdminRepository;
    private final EventVersionService eventVersionService;


    // Hashids 인코딩/디코딩
    public String encode(Long id) {
        return hashids.encode(id);
    }

    public Long decode(String hash) {
        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) return null;
        return decoded[0];
    }


    /*
     *  행사 생성
     *      1. 전체관리자가 행사 승인
     *      2. EventRequestDto에서 입력받은 email로 행사 관리자 계정 생성
     *      3. 생성된 행사 관리자 계정의 사용자 id를 managerId로 설정
     *      4. 행사 고유 코드(evnetCode) 생성 
     *          - 슬러그 + Hashid
     *      5. version 1로 설정
     */

    // 전체 관리자가 구독 생성
    @Transactional
    public EventResponseDto createEvent(EventRequestDto eventRequestDto) {

        log.info("행사 관리자 계정 생성 시작");
        // TODO: 행사 관리자 계정 생성 및 ID 받기
        Long managerId = 2L;    // NOTE: 임시로 하드코딩
        log.info("행사 관리자 계정 생성 완료");
        
        log.info("행사 생성 시작");
        Event event = new Event();

        // 행사 담당자 설정
        EventAdmin manager = eventAdminRepository.findById(managerId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사 관리자를 찾을 수 없습니다.", null));
        event.setManager(manager);
        log.info("담당자 설정 완료");

        // 행사 상태 UPCOMING으로 설정
        EventStatusCode status = eventStatusCodeRepository.findById(1)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "해당 코드 없음", null));
        event.setStatusCode(status);
        log.info("행사 상태 설정 완료");

        // 행사 생성 후 행사 ID로 고유 코드 생성하기 위해 임시값 저장
        event.setEventCode("TEMP"); 
        log.info("행사 고유 코드 임시 설정 완료");

        // 행사명 설정
        event.setTitleKr(eventRequestDto.getTitleKr());
        event.setTitleEng(eventRequestDto.getTitleEng());
        log.info("eventRequestDto eng: {}", eventRequestDto.getTitleEng());
        log.info("행사명 생성 완료");

        // Hashid로 고유 코드 생성
        Event savedEvent = eventRepository.save(event);
        String eventCode = encode(savedEvent.getEventId());
        savedEvent.setEventCode(eventCode);

        // 첫 번째 버전 생성
        log.info("첫 번째 버전 생성 for eventId: {}", savedEvent.getEventId());
        EventVersion firstVersion = eventVersionService.createEventVersion(savedEvent, 1L); // TODO: 전체 관리자 id 받아오는 걸로 수정하기
        log.info("첫 번째 버전 생성 완료");

        return EventResponseDto.builder()
                .message("행사 생성이 완료되었습니다.")
                .eventId(savedEvent.getEventId())
                .managerId(manager.getUser().getUserId())
                .eventCode(eventCode)
                .hidden(event.getHidden())
                .version(firstVersion.getVersionNumber())
                .build();
    }

    // 행사 목록 조회
    @Transactional(readOnly = true)
    public List<Long> getEvents() {    // TODO: EventSummaryResponseDto 로 변경하기
        return eventRepository.findAll().stream()
                .map(Event::getEventId)
                .collect(java.util.stream.Collectors.toList());
    }
        



    // 행사 업데이트
    @Transactional
    public EventResponseDto updateEvent(Long eventId, EventRequestDto updateRequest, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트를 찾을 수 없습니다.", null));

        // 현재 상태를 버전으로 저장
        log.info("버전 생성 for eventId: {}", eventId);
        EventVersion newVersion = eventVersionService.createEventVersion(event, managerId);

        // DTO의 내용으로 이벤트 정보를 업데이트
        log.info("이벤트 정보 업데이트 for eventId: {}", eventId);
        
        if(updateRequest.getTitleKr() != null) {  
            event.setTitleKr(updateRequest.getTitleKr());
        }

        if(updateRequest.getTitleEng() != null) {  
            event.setTitleEng(updateRequest.getTitleEng());
        }

        if(updateRequest.getHidden() != null) {
            event.setHidden(updateRequest.getHidden());
        }

        // 트랜잭션이 종료될 때 변경된 내용이 자동으로 DB에 저장

        return EventResponseDto.builder()
                .message("이벤트 정보가 성공적으로 업데이트되었습니다.")
                .eventId(event.getEventId())
                .eventCode(event.getEventCode())
                .managerId(event.getManager().getUser().getUserId())
                .hidden(event.getHidden())
                .version(newVersion.getVersionNumber())
                .build();
    }
}
