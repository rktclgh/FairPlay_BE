package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.scan.CheckInResponseDto;
import com.fairing.fairplay.qr.dto.scan.CheckOutResponseDto;
import com.fairing.fairplay.qr.dto.scan.GuestManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.scan.GuestQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberQrCheckInRequestDto;
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
  public CheckInResponseDto checkInWithQrByMember(MemberQrCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketEntryService.checkIn(dto, userDetails);
  }

  // 체크인 2
  @Transactional
  public CheckInResponseDto checkInWithManualByMember(MemberManualCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketEntryService.checkIn(dto, userDetails);
  }

  // 체크인 3
  @Transactional
  public CheckInResponseDto checkInWithQrByGuest(GuestQrCheckInRequestDto dto) {
    return qrTicketEntryService.checkIn(dto);
  }

  // 체크인 4
  @Transactional
  public CheckInResponseDto checkInWithManualByGuest(GuestManualCheckInRequestDto dto) {
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
  public CheckOutResponseDto checkOutWithQrByMember(MemberQrCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketExitService.checkOut(dto, userDetails);
  }

  // 체크아웃 2
  @Transactional
  public CheckOutResponseDto checkOutWithManualByMember(MemberManualCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    return qrTicketExitService.checkOut(dto, userDetails);
  }

  // 체크아웃 3
  @Transactional
  public CheckOutResponseDto checkOutWithQrByGuest(GuestQrCheckInRequestDto dto) {
    return qrTicketExitService.checkOut(dto);
  }

  // 체크아웃 4
  @Transactional
  public CheckOutResponseDto checkOutWithManualByGuest(GuestManualCheckInRequestDto dto) {
    return qrTicketExitService.checkOut(dto);
  }

}
