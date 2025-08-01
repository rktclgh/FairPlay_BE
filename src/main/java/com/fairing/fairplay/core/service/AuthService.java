package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.dto.LoginRequest;
import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    private final String kakaoClientId;
    private final String kakaoRedirectUri;
    private final String kakaoClientSecret;

    public AuthService(
            UserRepository userRepository,
            UserRoleCodeRepository userRoleCodeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            @Value("${kakao.client-id}") String kakaoClientId,
            @Value("${kakao.redirect-uri}") String kakaoRedirectUri,
            @Value("${kakao.client-secret:}") String kakaoClientSecret
    ) {
        this.userRepository = userRepository;
        this.userRoleCodeRepository = userRoleCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.kakaoClientId = kakaoClientId;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.kakaoClientSecret = kakaoClientSecret;
    }

    // 로그인 + JWT 발급
    public LoginResponse login(LoginRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getDeletedAt() != null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRoleCode().getName()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );

        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiry()
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    // 리프레시 토큰 재발급
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String redisToken = refreshTokenService.getRefreshToken(userId);

        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다.");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getDeletedAt() != null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRoleCode().getName()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );

        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiry()
        );

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public LoginResponse kakaoLogin(String code) {
        // 1. 카카오 토큰 요청
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            params.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token",
                tokenRequest,
                String.class
        );

        String accessToken;
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode node = om.readTree(tokenResponse.getBody());
            accessToken = node.get("access_token").asText();
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "카카오 토큰 발급 실패");
        }

        // 2. 카카오 유저 정보 요청
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.set("Authorization", "Bearer " + accessToken);
        HttpEntity<?> userInfoRequest = new HttpEntity<>(userInfoHeaders);

        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                userInfoRequest,
                String.class
        );

        String kakaoId = null;
        String nickname = null;
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode userNode = om.readTree(userInfoResponse.getBody());
            kakaoId = userNode.get("id").asText(); // 카카오 고유 userId
            JsonNode kakaoAccount = userNode.get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.has("profile")) {
                nickname = kakaoAccount.get("profile").get("nickname").asText();
            }
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "카카오 유저 정보 파싱 실패");
        }

        if (kakaoId == null || kakaoId.isBlank()) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "카카오 아이디 수신 실패");
        }

        // 임시 이메일 생성 (kakao_1234567890)
        final String tempEmail = "kakao_" + kakaoId;
        final String finalNickname = nickname != null ? nickname : "kakaoUser";

        Users user = userRepository.findByEmail(tempEmail)
                .orElseGet(() -> {
                    // 기본 권한 COMMON 코드 엔티티 할당 (없으면 예외)
                    UserRoleCode userRole = userRoleCodeRepository.findByCode("COMMON")
                            .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "기본 권한 코드(COMMON)가 없습니다."));
                    // 신규 소셜 회원가입
                    Users newUser = Users.builder()
                            .email(tempEmail)
                            .nickname(finalNickname)
                            .password(passwordEncoder.encode("kakao_social")) // 소셜로그인용 비번(임의)
                            .roleCode(userRole)
                            .name(finalNickname)
                            .phone("01000000000") // 더미값, 추후 수정
                            .build();
                    userRepository.save(newUser);
                    return newUser;
                });

        // JWT, refreshToken 발급
        String ourAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRoleCode().getName()
        );
        String ourRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );
        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                ourRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiry()
        );

        return new LoginResponse(ourAccessToken, ourRefreshToken);
    }

}
