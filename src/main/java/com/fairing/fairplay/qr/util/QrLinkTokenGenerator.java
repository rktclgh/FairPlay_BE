package com.fairing.fairplay.qr.util;

import org.springframework.stereotype.Component;

// QR 티켓링크용 토큰 관련 유틸 클래스
// 임시 주석 처리
// 의존성 jjwt 추가 필요
@Component
public class QrLinkTokenGenerator {
//
//  @Value("${qr.secret_key}")
//  private static String SECRET_KEY;
//
//  public static String generateToken(QrTicketRequestDto dto) {
//    long now = System.currentTimeMillis();
//    Date issuedAt = new Date(now);
//
//    return Jwts.builder()
//        .claim("reservationId", dto.getReservationId())
//        .claim("attendeeId", dto.getAttendeeId())
//        .claim("eventId", dto.getEventId())
//        .claim("ticketId", dto.getTicketId())
//        .setIssuedAt(issuedAt)
//        .setExpiration(expiryDate)
//        .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
//        .compact();
//  }
//
//  // 링크 조회 시 실행되는 함수
//  public static Claims getClaimsFromToken(String token) {
//    return Jwts.parser()
//        .setSigningKey(SECRET_KEY)
//        .parseClaimsJws(token)
//        .getBody();
//  }
}
