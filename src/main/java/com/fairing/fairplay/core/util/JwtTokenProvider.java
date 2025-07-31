package com.fairing.fairplay.core.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityInMillis,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityInMillis
    ) {
        this.secretKey = secret;
        this.accessTokenValidityInMillis = accessTokenValidityInMillis;
        this.refreshTokenValidityInMillis = refreshTokenValidityInMillis;
    }

    // 액세스 토큰 발급 (userId, email, 권한명)
    public String generateAccessToken(Long userId, String email, String roleName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityInMillis);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", roleName)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
    }

    // 리프레시 토큰 발급 (userId, email)
    public String generateRefreshToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInMillis);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
    }

    // 액세스 토큰 만료시간(ms)
    public long getAccessTokenExpiry() {
        return accessTokenValidityInMillis;
    }

    // 리프레시 토큰 만료시간(ms)
    public long getRefreshTokenExpiry() {
        return refreshTokenValidityInMillis;
    }

    // 유저ID 추출
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 이메일 추출
    public String getEmail(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("email");
    }

    // 권한명 추출
    public String getRole(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("role");
    }

    // 만료여부
    public boolean isExpired(String token) {
        try {
            Date exp = getClaims(token).getExpiration();
            return exp.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    // 유효성 검사
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Claims 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
}
