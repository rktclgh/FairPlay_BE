package com.fairing.fairplay.qr.util;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeValidator {

  private final Hashids hashids;
  private final QrTicketRepository qrTicketRepository;

  // QrUrlToken 검증
  public QrTicketRequestDto decodeToDto(String token) {
    if (token == null || token.trim().isEmpty()) {
      throw new CustomException(HttpStatus.BAD_REQUEST,"QR 링크 토큰이 비어 있습니다.");
    }

    try {
      long[] numbers = decodeToken(token);
      // 디코딩 결과 배열 길이 체크
      if (numbers.length < 4) {
        throw new CustomException(HttpStatus.BAD_REQUEST,"유효하지 않은 QR 토큰 형식입니다.");
      }

      if (numbers[0] == 0 && numbers[1] == 0 && numbers[2] == 0 && numbers[3] == 0) {
        throw new CustomException(HttpStatus.BAD_REQUEST,"유효하지 않은 QR 토큰 데이터입니다.");
      }

      return QrTicketRequestDto.builder()
          .reservationId(numbers[0] == 0 ? null : numbers[0])
          .attendeeId(numbers[1] == 0 ? null : numbers[1])
          .eventId(numbers[2] == 0 ? null : numbers[2])
          .ticketId(numbers[3] == 0 ? null : numbers[3])
          .build();
    } catch (Exception e) {
      throw new CustomException(HttpStatus.BAD_REQUEST,"토큰 디코딩 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  // 수동코드 검증
  public void validateManualCode(String manualCode) {
    if (manualCode == null) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "수동 코드를 입력해 주세요.");
    }
    String pattern = "^[A-Z]{4}-[A-Z]{4}$"; // 예시: 대문자 4글자-대문자 4글자
    if (!manualCode.matches(pattern)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "수동 코드 형식이 올바르지 않습니다.");
    }
  }

  // 토큰 문자열 -> long[]으로 변환
  private long[] decodeToken(String token) {
    return hashids.decode(token);
  }
}
