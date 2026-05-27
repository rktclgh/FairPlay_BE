package com.fairing.fairplay.history.aspect;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.entity.ChangeHistory;
import com.fairing.fairplay.history.etc.ChangeAccount;
import com.fairing.fairplay.history.etc.ChangeBanner;
import com.fairing.fairplay.history.etc.ChangeContent;
import com.fairing.fairplay.history.etc.ChangeEvent;
import com.fairing.fairplay.history.etc.ChangeTargetId;
import com.fairing.fairplay.history.repository.ChangeHistoryRepository;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class AccessAspect {

    private final JwtTokenProvider jwtTokenProvider;

    private final UserRepository userRepository;

    private final LoginHistoryService loginHistoryService;

    private final ChangeHistoryRepository changeHistoryRepository;

    public AccessAspect(UserRepository userRepository, LoginHistoryService loginHistoryService,
            ChangeHistoryRepository changeHistoryRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.loginHistoryService = loginHistoryService;
        this.changeHistoryRepository = changeHistoryRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Pointcut("execution(* com.fairing.fairplay.core.service.AuthService.login(..)) || execution(* com.fairing.fairplay.core.service.AuthService.kakaoLogin(..))")
    public void login() {
    }

    @Pointcut("execution(* com.fairing.fairplay.admin.service.SuperAdminService.disableUser(..))")
    public void disableUser() {
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeTemplate)")
    public Object aroundChangeTemplate(ProceedingJoinPoint joinPoint) throws Throwable {
        String content = requireChangeContent(joinPoint);
        Users executor = requireCurrentExecutor(joinPoint);

        Object result = joinPoint.proceed();
        saveChangeHistory(executor, null, "템플릿 수정", content);
        return result;
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeAccount)")
    public Object aroundChangeAccount(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeAccount changeAccount = method.getAnnotation(ChangeAccount.class);
        String changeString = changeAccount.value();
        Long targetId = requireChangeTargetId(joinPoint);
        Users executor = requireCurrentExecutor(joinPoint);

        Object result = joinPoint.proceed();
        saveChangeHistory(executor, targetId, "계정 정보 수정", changeString);
        return result;
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeBanner)")
    public Object aroundChangeBanner(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeBanner changeBanner = method.getAnnotation(ChangeBanner.class);
        String changeString = changeBanner.value();

        Long bannerId = requireChangeTargetId(joinPoint);
        Users executor = requireCurrentExecutor(joinPoint);
        String content = changeString + " (배너 ID: " + bannerId + ")";

        Object result = joinPoint.proceed();
        saveChangeHistory(executor, bannerId, "배너 정보 수정", content);
        return result;
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeEvent)")
    public Object aroundChangeEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeEvent changeEvent = method.getAnnotation(ChangeEvent.class);
        String changeString = changeEvent.value();

        Long targetId = requireChangeTargetId(joinPoint);
        Users executor = requireCurrentExecutor(joinPoint);

        Object result = joinPoint.proceed();
        saveChangeHistory(executor, targetId, "행사 정보 수정", changeString);
        return result;
    }

    @Around("disableUser()")
    public Object aroundDisableUser(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

    @AfterReturning(pointcut = "login()", returning = "response")
    public void aroundLogin(LoginResponse response) {

        Long userId = jwtTokenProvider.getUserId(response.getAccessToken());
        LocalDateTime loginTime = LocalDateTime.now();
        LoginHistoryDto loginHistory = new LoginHistoryDto();
        Users user = userRepository.findById(userId).orElseThrow();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("잘못된 연결");
        }
        HttpServletRequest httpRequest = attributes.getRequest();
        String ip = getClientIpAddress(httpRequest);

        String userAgent = httpRequest.getHeader("User-Agent");
        loginHistory.setUserId(userId);
        loginHistory.setIp(ip);
        loginHistory.setUser_role_code_id(user.getRoleCode().getId());
        loginHistory.setLoginTime(loginTime);
        loginHistory.setUserAgent(userAgent);

        loginHistoryService.saveLoginHistory(loginHistory);

    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return null; // 인증되지 않은 경우 null 반환
    }

    private Optional<Long> resolveChangeTargetId(ProceedingJoinPoint joinPoint) {
        return resolveAnnotatedArgument(joinPoint, ChangeTargetId.class).flatMap(this::toLong);
    }

    private Optional<Object> resolveChangeContent(ProceedingJoinPoint joinPoint) {
        return resolveAnnotatedArgument(joinPoint, ChangeContent.class);
    }

    private Optional<Object> resolveAnnotatedArgument(ProceedingJoinPoint joinPoint,
            Class<? extends java.lang.annotation.Annotation> annotationType) {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return Optional.empty();
        }

        Object[] args = joinPoint.getArgs();
        java.lang.annotation.Annotation[][] annotations = methodSignature.getMethod().getParameterAnnotations();
        int limit = Math.min(args.length, annotations.length);
        for (int i = 0; i < limit; i++) {
            for (java.lang.annotation.Annotation annotation : annotations[i]) {
                if (annotation.annotationType().equals(annotationType)) {
                    return Optional.ofNullable(args[i]);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Long> toLong(Object value) {
        if (value instanceof Long longValue) {
            return Optional.of(longValue);
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Optional.of(Long.parseLong(stringValue));
            } catch (NumberFormatException e) {
                log.warn("감사 로그 targetId 변환 실패: {}", stringValue);
            }
        }
        return Optional.empty();
    }

    private Long requireChangeTargetId(ProceedingJoinPoint joinPoint) {
        return resolveChangeTargetId(joinPoint)
                .orElseThrow(() -> new IllegalStateException(
                        joinPoint.getSignature().toShortString() + " 감사 로그 targetId 바인딩 누락"));
    }

    private String requireChangeContent(ProceedingJoinPoint joinPoint) {
        Object content = resolveChangeContent(joinPoint)
                .orElseThrow(() -> new IllegalStateException(
                        joinPoint.getSignature().toShortString() + " 감사 로그 content 바인딩 누락"));
        return content != null ? content.toString() : null;
    }

    private Users requireCurrentExecutor(ProceedingJoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException(joinPoint.getSignature().toShortString() + " 감사 로그 인증 사용자 누락");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        joinPoint.getSignature().toShortString() + " 감사 로그 실행 사용자 조회 실패: " + userId));
    }

    private void saveChangeHistory(Users executor, Long targetId, String targetType, String content) {
        ChangeHistory changeHistory = ChangeHistory.builder()
                .user(executor)
                .targetId(targetId)
                .targetType(targetType)
                .content(content)
                .modifyTime(LocalDateTime.now())
                .build();
        changeHistoryRepository.save(changeHistory);
    }

    /**
     * 클라이언트의 실제 IP 주소를 가져옵니다.
     * 프록시, 로드밸런서, CDN 등을 통과한 경우에도 실제 클라이언트 IP를 찾습니다.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // 1. X-Forwarded-For 헤더 확인 (가장 일반적)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // X-Forwarded-For는 쉼표로 구분된 IP 목록일 수 있음 (첫 번째가 실제 클라이언트 IP)
            return ipAddress.split(",")[0].trim();
        }

        // 2. Proxy-Client-IP 헤더 확인
        ipAddress = request.getHeader("Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // 3. WL-Proxy-Client-IP 헤더 확인 (WebLogic)
        ipAddress = request.getHeader("WL-Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // 4. HTTP_CLIENT_IP 헤더 확인
        ipAddress = request.getHeader("HTTP_CLIENT_IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // 5. HTTP_X_FORWARDED_FOR 헤더 확인
        ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress.split(",")[0].trim();
        }

        // 6. X-Real-IP 헤더 확인 (Nginx 등에서 사용)
        ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // 7. X-Cluster-Client-IP 헤더 확인
        ipAddress = request.getHeader("X-Cluster-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // 8. 모든 헤더에서 찾지 못한 경우 기본 RemoteAddr 사용
        ipAddress = request.getRemoteAddr();

        // IPv6 localhost를 IPv4로 변환
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        return ipAddress;
    }

}
