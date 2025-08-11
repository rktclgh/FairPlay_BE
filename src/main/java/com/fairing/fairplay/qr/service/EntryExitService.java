package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.GuestManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.GuestQrCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberQrCheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntryExitService {

  private final QrTicketEntryService qrTicketEntryService;
  private final QrTicketExitService qrTicketExitService;

  /*
   * 체크인
   * 1. 회원+QR
   * 2. 회원+수동
   * 3. 비회원+QR
   * 4. 비회원+수동
   *
   */

  // 체크인 1
  @Transactional
  public CheckResponseDto checkInWithQrByMember(MemberQrCheckRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketEntryService.checkIn(dto, userDetails);
  }

  // 체크인 2
  @Transactional
  public CheckResponseDto checkInWithManualByMember(MemberManualCheckRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketEntryService.checkIn(dto, userDetails);
  }

  // 체크인 3
  @Transactional
  public CheckResponseDto checkInWithQrByGuest(GuestQrCheckRequestDto dto) {
    return qrTicketEntryService.checkIn(dto);
  }

  // 체크인 4
  @Transactional
  public CheckResponseDto checkInWithManualByGuest(GuestManualCheckRequestDto dto) {
    return qrTicketEntryService.checkIn(dto);
  }

  /*
   * 퇴장
   * 1. 회원 + QR
   * 2. 회원 + 수동
   * 3. 비회원 + QR
   * 4. 비회원 + 수동
   * */

  // 체크아웃 1
  @Transactional
  public CheckResponseDto checkOutWithQrByMember(MemberQrCheckRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketExitService.checkOut(dto, userDetails);
  }

  // 체크아웃 2
  @Transactional
  public CheckResponseDto checkOutWithManualByMember(MemberManualCheckRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketExitService.checkOut(dto, userDetails);
  }

  // 체크아웃 3
  @Transactional
  public CheckResponseDto checkOutWithQrByGuest(GuestQrCheckRequestDto dto) {
    return qrTicketExitService.checkOut(dto);
  }

  // 체크아웃 4
  @Transactional
  public CheckResponseDto checkOutWithManualByGuest(GuestManualCheckRequestDto dto) {
    return qrTicketExitService.checkOut(dto);
  }

}
