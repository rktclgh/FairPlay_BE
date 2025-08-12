package com.fairing.fairplay.qr.util;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeGenerator {

  private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 헷갈리는 문자 제외
  private static final int CODE_LENGTH = 8;

  private static final SecureRandom random = new SecureRandom();
  private final StringRedisTemplate redisTemplate;
  private final Hashids hashids;

  // QR 고유 코드 생성 ex.nk2s0
  public String generateQrCode(QrTicket qrTicket) {
    ZonedDateTime zdt = qrTicket.getExpiredAt().atZone(ZoneId.of("Asia/Seoul"));
    long epochSeconds = zdt.toEpochSecond();

    long randomSalt = random.nextLong(1_000_000, 9_999_999);

    long[] numbers = new long[]{
        safeLong(qrTicket.getId()),
        safeLong(qrTicket.getAttendee().getId()),
        epochSeconds,
        randomSalt
    };

    return hashids.encode(numbers);
  }

  // 수동 코드 생성 ex. ABCD-EFGH
  public String generateManualCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH + 1); // '-' 하나 추가

    for (int i = 0; i < CODE_LENGTH; i++) {
      int idx = random.nextInt(CHAR_POOL.length());
      sb.append(CHAR_POOL.charAt(idx));

      // 4번째 글자 뒤에만 '-' 삽입 (0부터 시작이니까 i == 3)
      if (i == 3) {
        sb.append('-');
      }
    }
    return sb.toString();
  }

  // QR 티켓 조회 화면 토큼 생성 ex. nk2s0
  public String generateQrUrlToken(QrTicketRequestDto dto) {

    long[] numbers = new long[]{
        safeLong(dto.getReservationId()),
        safeLong(dto.getAttendeeId()),
        safeLong(dto.getEventId()),
        safeLong(dto.getTicketId())
    };

    return hashids.encode(numbers);
  }

  // 티켓 코드 생성 ex. FR2025-20250802-0012
  // 하루마다 예약건에 대한 시퀀스 초기화
  // redis 시퀀스 이용해 설정
  public String generateTicketNo(String eventCode) {
    try {
      String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE); // "20250804"
      String key = "ticketSeq:" + eventCode + ":" + date;

      log.info("🚩 key 생성됨: {}", key);
      log.info("🚩 redisTemplate: {}", redisTemplate); // null 여부 확인

      Long seq = redisTemplate.opsForValue().increment(key);
      log.info("🚩 Redis에서 시퀀스 값: {}", seq);

      if (seq == 1) {
        redisTemplate.expire(key, Duration.ofDays(1));
        log.info("🚩 키 만료시간 설정됨: {} - 1일", key);
      }

      return String.format("%s-%s-%04d", eventCode, date, seq);
    } catch (Exception e) {
      log.error("❌ generateTicketNo 예외 발생", e); // 원래 에러를 로그로 남기자!
      throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
          "티켓 번호 생성 중 오류가 발생했습니다: " + e.getMessage());
    }

  }

  // DTO 값이 null일 때 오류 방지용
  private long safeLong(Long val) {
    return val == null ? 0L : val;
  }
}
