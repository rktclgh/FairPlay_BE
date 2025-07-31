package com.fairing.fairplay.core.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key secretKey;
    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityInMillis,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityInMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMillis = accessTokenValidityInMillis;
        this.refreshTokenValidityInMillis = refreshTokenValidityInMillis;
    }

    // 액세스 토큰 발급
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityInMillis);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 리프레시 토큰 발급
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInMillis);

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 유저ID 추출
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 토큰에서 권한 추출
    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    // 토큰 만료 체크
    public boolean isExpired(String token) {
        try {
            Date exp = getClaims(token).getExpiration();
            return exp.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Claims 추출 (파싱)
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


//    // Claims 추출 (파싱)
//    private Claims getClaims(String token) {
//        // 여기만 바꿔주면 됨!
//        JwtParser parser = Jwts.parser().setSigningKey(secretKey);
//        // 아래처럼 .parseClaimsJws(token) 사용 (0.12.x에서는 parserBuilder 또는 parser 둘 다 됨)
//        return parser.parseClaimsJws(token).getBody();
//    }

}
