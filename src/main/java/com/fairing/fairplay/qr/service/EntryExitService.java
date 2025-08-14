package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.qr.dto.scan.AdminForceCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.ManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntryExitService {

  private final QrTicketEntryService qrTicketEntryService;
  private final QrTicketExitService qrTicketExitService;

  // QR 체크인
  @Transactional
  public CheckResponseDto checkInWithQr(QrCheckRequestDto dto) {
    return qrTicketEntryService.checkInWithQr(dto);
  }

  // 수동 코드 체크인
  @Transactional
  public CheckResponseDto checkInWithManual(ManualCheckRequestDto dto) {
    return qrTicketEntryService.checkInWithManual(dto);
  }

  // QR 체크아웃
  @Transactional
  public CheckResponseDto checkOutWithQr(QrCheckRequestDto dto) {
    return qrTicketExitService.checkOutWithQr(dto);
  }

  // 수동 코드 체크아웃
  @Transactional
  public CheckResponseDto checkOutWithManual(ManualCheckRequestDto dto) {
    return qrTicketExitService.checkOutWithManual(dto);
  }

  @Transactional
  public CheckResponseDto adminForceCheck(AdminForceCheckRequestDto dto) {
    return qrTicketEntryService.adminForceCheck(dto);
  }
}
