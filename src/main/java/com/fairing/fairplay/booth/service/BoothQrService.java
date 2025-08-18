package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.booth.dto.BoothExperienceReservationResponseDto;
import com.fairing.fairplay.booth.dto.BoothExperienceStatusUpdateDto;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import com.fairing.fairplay.booth.repository.BoothExperienceStatusCodeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.service.QrTicketEntryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoothQrService {

  private final UserRepository userRepository;
  private final BoothExperienceStatusCodeRepository boothExperienceStatusCodeRepository;
  private final BoothExperienceService boothExperienceService;
  private final QrTicketEntryService qrTicketEntryService;

  // QR 티켓을 통한 부스 입장 처리
  @Transactional
  public CheckResponseDto checkIn(CustomUserDetails userDetails, BoothEntryRequestDto dto) {
    // 1. 사용자 검증 - 로그인 사용자 확인, 사용자 조회
    Users user = validateUser(userDetails);
    // 2. 필수 값 확인
    if (dto.getBoothReservationId() == null || dto.getBoothId() == null || dto.getEventId() == null
        || (dto.getManualCode() == null && dto.getQrCode() == null)) {
      throw new CustomException(HttpStatus.BAD_REQUEST,
          "예약 ID, 부스 ID, 행사 ID, QR 코드 또는 수동 코드 중 필수 값이 누락되었습니다.");
    }
    // 3. 예약 검증 - 부스 예약 조회, 예약 상태 확인, 행사, 부스 일치 여부 확인
    BoothExperienceReservation boothExperienceReservation = boothExperienceService.validateReservation(
        dto);

    // 4. QR 티켓 검증 - QR 코드 또는 수동 코드로 QR 티켓 조회, QR 티켓 소유자 == 로그인 사용자 확인
    QrTicket qrTicket = qrTicketEntryService.validateQrTicket(user, dto, boothExperienceReservation);

    // 5. 상태 IN_PROGRESS 로 변경
    BoothExperienceStatusCode boothExperienceStatusCode = boothExperienceStatusCodeRepository.findByCode(
        "IN_PROGRESS").orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
            "부스 예약 상태 코드(IN_PROGRESS)를 찾을 수 없습니다.")
    );
    BoothExperienceStatusUpdateDto boothExperienceStatusUpdateDto = BoothExperienceStatusUpdateDto.builder()
        .statusCode(boothExperienceStatusCode.getCode())
        .notes("QR 스캔을 통한 부스 입장 처리")
        .build();
    BoothExperienceReservationResponseDto boothExperienceReservationResponseDto = boothExperienceService.updateReservationStatus(
        boothExperienceReservation.getReservationId(),
        boothExperienceStatusUpdateDto);
    return qrTicketEntryService.processQrEntry(qrTicket);
  }

  private Users validateUser(CustomUserDetails userDetails) {
    if (userDetails == null) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "로그인한 사용자만 QR 티켓으로 입장할 수 있습니다.");
    }
    return userRepository.findById(userDetails.getUserId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다.")
    );
  }
}



