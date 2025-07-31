package com.fairing.fairplay.qr.util;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {

  private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 헷갈리는 문자 제외
  private static final int CODE_LENGTH = 8;

  private static final SecureRandom random = new SecureRandom();

  public String generateRandomToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

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
}
