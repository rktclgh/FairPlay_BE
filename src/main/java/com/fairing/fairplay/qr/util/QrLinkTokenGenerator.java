package com.fairing.fairplay.qr.util;

import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.stereotype.Component;

// QR 티켓링크용 토큰 관련 유틸 클래스
@Component
@RequiredArgsConstructor
public class QrLinkTokenGenerator {

  private final Hashids hashids;

  // 암호화한 문자열 토큰 생성
  public String generateToken(QrTicketRequestDto dto) {

    // hashids는 long 배열만 받음
    // 숫자 배열을 인코딩해 짧고 URL 안전한 문자열로 만듦
    // ex. nk2s0
    long[] numbers = new long[]{
        safeLong(dto.getReservationId()),
        safeLong(dto.getAttendeeId()),
        safeLong(dto.getEventId()),
        safeLong(dto.getTicketId())
    };

    return hashids.encode(numbers);
  }

  // 암호화된 문자열을 DTO 객체로 만듦
  public QrTicketRequestDto decodeToDto(String token) {
    if(token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("QR 링크 토큰이 비어 있습니다.");
    }

    try{
      long[] numbers = decodeToken(token);
      // 디코딩 결과 배열 길이 체크
      if (numbers.length < 4) {
        throw new IllegalArgumentException("유효하지 않은 QR 토큰 형식입니다.");
      }

      if(numbers[0] == 0 && numbers[1] == 0 && numbers[2] == 0 && numbers[3] == 0){
        throw new IllegalArgumentException("유효하지 않은 QR 토큰 데이터입니다.");
      }

      return QrTicketRequestDto.builder()
          .reservationId(numbers[0] == 0 ? null : numbers[0])
          .attendeeId(numbers[1] == 0 ? null : numbers[1])
          .eventId(numbers[2] == 0 ? null : numbers[2])
          .ticketId(numbers[3] == 0 ? null : numbers[3])
          .build();
    }catch(Exception e){
      throw new IllegalArgumentException("토큰 디코딩 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  // 토큰 문자열 -> long[]으로 변환
  private long[] decodeToken(String token) {
    return hashids.decode(token);
  }

  // DTO 값이 null일 때 오류 방지용
  private long safeLong(Long val) {
    return val == null ? 0L : val;
  }
}
