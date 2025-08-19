package com.fairing.fairplay.history.aspect;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

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
import com.fairing.fairplay.history.etc.ChangeEvent;
import com.fairing.fairplay.history.repository.ChangeHistoryRepository;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
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
        joinPoint.getArgs();
        Long userId = getCurrentUserId(); // 테스트용 하드코딩, getCurrentId 사용 예정
        String targetHtml = joinPoint.getArgs()[0].toString(); // Assuming the first argument is the template name
        Users executor = userRepository.findById(userId).orElseThrow();
        ChangeHistory changeHistory = ChangeHistory.builder()
                .user(executor)
                .targetType("템플릿 수정")
                .content(targetHtml)
                .modifyTime(LocalDateTime.now())
                .build();
        changeHistoryRepository.save(changeHistory);
        return joinPoint.proceed();
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeAccount)")
    public Object aroundChangeAccount(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeAccount changeAccount = method.getAnnotation(ChangeAccount.class);
        String changeString = changeAccount.value();
        Long userId = getCurrentUserId(); // 테스트용 하드코딩, getCurrentId 사용 예정
        Long targetId = (Long) joinPoint.getArgs()[1];
        Users executor = userRepository.findById(userId).orElseThrow();
        Users target = userRepository.findById(targetId).orElseThrow();
        ChangeHistory changeHistory = ChangeHistory.builder()
                .user(executor)
                .targetId(target.getUserId())
                .targetType("계정 정보 수정")
                .content(changeString)
                .modifyTime(LocalDateTime.now())
                .build();
        changeHistoryRepository.save(changeHistory);
        return joinPoint.proceed();
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeBanner)")
    public Object aroundChangeBanner(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeBanner changeBanner = method.getAnnotation(ChangeBanner.class);
        String changeString = changeBanner.value();

        Long userId = getCurrentUserId(); // 테스트용 하드코딩, getCurrentId 사용 예정
        Long targetId = (Long) joinPoint.getArgs()[1];
        Users executor = userRepository.findById(userId).orElseThrow();
        Users target = userRepository.findById(targetId).orElseThrow();
        ChangeHistory changeHistory = ChangeHistory.builder()
                .user(executor)
                .targetId(target.getUserId())
                .targetType("배너 정보 수정")
                .content(changeString)
                .modifyTime(LocalDateTime.now())
                .build();
        changeHistoryRepository.save(changeHistory);
        return joinPoint.proceed();
    }

    @Around("@annotation(com.fairing.fairplay.history.etc.ChangeEvent)")
    public Object aroundChangeEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ChangeEvent changeEvent = method.getAnnotation(ChangeEvent.class);
        String changeString = changeEvent.value();

        Long userId = getCurrentUserId(); // 테스트용 하드코딩, getCurrentId 사용 예정
        Long targetId = (Long) joinPoint.getArgs()[1];
        Users executor = userRepository.findById(userId).orElseThrow();
        Users target = userRepository.findById(targetId).orElseThrow();
        ChangeHistory changeHistory = ChangeHistory.builder()
                .user(executor)
                .targetId(target.getUserId())
                .targetType("행사 정보 수정")
                .content(changeString)
                .modifyTime(LocalDateTime.now())
                .build();
        changeHistoryRepository.save(changeHistory);
        return joinPoint.proceed();
    }

    @Around("disableUser()")
    public Object aroundDisableUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long targetId = (Long) args[1];
        Users user = userRepository.findById(targetId).orElseThrow();
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
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
        String ip = httpRequest.getRemoteAddr();

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
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return null; // 인증되지 않은 경우 null 반환
    }

}
