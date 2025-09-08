package com.fairing.fairplay.attendeeform.service;

import com.fairing.fairplay.attendeeform.entity.AttendeeForm;
import com.fairing.fairplay.attendeeform.repository.AttendeeFormRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeFormBatchService {

  private final AttendeeFormRepository attendeeFormRepository;

  // 만료 날짜가 오늘이고 아직 만료처리되지 않은 공유 폼 링크 조회
  // 당일 예약이거나 행사 전날 예약했을 경우엔 제외
  public List<AttendeeForm> fetchExpiredBatch(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    LocalDate now = LocalDate.now(); // 2025-08-10
    LocalDateTime startDate = now.atStartOfDay();
    LocalDateTime endDate = now.plusDays(1).atStartOfDay(); // 11
    log.info("fetchExpiredBatch startDate: {}, endDate: {}", startDate, endDate);

    return attendeeFormRepository.findAllExpiredExceptTodayReservations(endDate, now,  pageable);
  }

  // 공유 폼 링크 만료 -> 스케줄러 자동 실행
  @Transactional
  public void expiredToken(List<AttendeeForm> attendeeForms) {
    // 폼링크 자동 만료
    attendeeForms.forEach(attendeeForm -> {
      attendeeForm.setExpired(true);
    });

    log.info("expiredToken: {}", attendeeForms.size());
    attendeeFormRepository.saveAll(attendeeForms);
    attendeeFormRepository.flush();
  }

  // 만료된 정보 삭제
  public void deleteAttendeeForm(AttendeeForm attendeeForm) {
    // 취소된 티켓일 경우
    if (attendeeForm.getReservation().isCanceled()) {
      attendeeForm.setExpired(true);
    }
  }
}
