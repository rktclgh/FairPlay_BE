package com.fairing.fairplay.qr.util;

import com.fairing.fairplay.common.exception.CustomException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeGenerator {

  private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 헷갈리는 문자 제외
  private static final int CODE_LENGTH = 8;

  private static final SecureRandom random = new SecureRandom();
  private final RedisTemplate<Object, Object> redisTemplate;

  // QR 코드 생성 ex. 550e8400-e29b-41d4-a716-446655440000
  public String generateRandomToken() {
    return UUID.randomUUID().toString().replace("-", "");
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

  // 티켓 코드 생성 ex. FR2025-20250802-0012
  public String generateTicketNo(String eventCode) {
    try {
      String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE); // "20250802"
      String key = "ticketSeq:" + eventCode + ":" + date;
      Long seq = redisTemplate.opsForValue().increment(key); // redis에서 자동 증가
      if (seq == 1) {
        redisTemplate.expire(key, Duration.ofDays(1)); // 하루 단위로 시퀀스 초기화
      }
      return String.format("%s-%s-%04d", eventCode, date, seq);
    } catch (Exception e) {
      throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "티켓 번호 생성 중 오류가 발생했습니다.");
    }

  }
}
