package com.fairing.fairplay.attendee.service;

import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.service.ShareTicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*참석자 서비스*/
@Service
@RequiredArgsConstructor
public class AttendeeService {

  private final AttendeeRepository attendeeRepository;
  private final AttendeeTypeCodeRepository attendeeTypeCodeRepository;
  private final ShareTicketService shareTicketService;

  // 대표자 정보 저장
  @Transactional
  public void savePrimary(AttendeeSaveRequestDto dto) {
    saveAttendee("PRIMARY", dto, dto.getReservationId());
  }

  // 동반자 정보 저장
  @Transactional
  public void saveGuest(String token, AttendeeSaveRequestDto dto) {
    ShareTicket shareTicket = shareTicketService.validateAndUseToken(token);
    saveAttendee("GUEST", dto, shareTicket.getReservationId());
  }

  // 참석자 전체 조회
  public List<Attendee> findAll() {
    return attendeeRepository.findAll();
  }

  // DB save 로직 분리
  private void saveAttendee(String attendeeType, AttendeeSaveRequestDto dto, Long reservationId) {
    AttendeeTypeCode attendeeTypeCode = attendeeTypeCodeRepository.findByCode(attendeeType)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 타입입니다."));

    Attendee attendee = Attendee.builder()
        .name(dto.getName())
        .attendeeTypeCode(attendeeTypeCode)
        .phone(dto.getPhone())
        .email(dto.getEmail())
        .reservationId(reservationId)
        .build();
    attendeeRepository.save(attendee);
  }

}
