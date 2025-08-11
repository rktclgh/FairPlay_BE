package com.fairing.fairplay.history.aspect;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fairing.fairplay.core.dto.LoginRequest;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AccessAspect {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @Pointcut("execution(* com.fairing.fairplay.core.service.AuthService.login(..))")
    public void login() {
    }

    @Pointcut("execution(* com.fairing.fairplay.admin.service.SuperAdminService.disableUser(..))")
    public void disableUser() {
    }

    @Around("disableUser()")
    public Object aroundDisableUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long userId = (Long) args[0];
        Long targetId = (Long) args[1];
        Users user = userRepository.findById(targetId).orElseThrow();
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        return joinPoint.proceed();
    }

    @Around("login()")
    public Object aroundLogin(ProceedingJoinPoint joinPoint) throws Throwable {

        Object[] args = joinPoint.getArgs();
        LoginRequest request = (LoginRequest) args[0];
        String email = request.getEmail();
        LocalDateTime loginTime = LocalDateTime.now();
        LoginHistoryDto loginHistory = new LoginHistoryDto();
        Users user = userRepository.findByEmail(email).orElseThrow();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("잘못된 연결");
        }
        HttpServletRequest httpRequest = attributes.getRequest();
        String ip = httpRequest.getRemoteAddr();

        String userAgent = httpRequest.getHeader("User-Agent");
        loginHistory.setUserId(user.getUserId());
        loginHistory.setIp(ip);
        loginHistory.setUser_role_code_id(user.getRoleCode().getId());
        loginHistory.setLoginTime(loginTime);
        loginHistory.setUserAgent(userAgent);

        Object res = null;
        try {
            res = joinPoint.proceed();
            return res;
        } catch (Exception e) {
            throw e;
        } finally {
            loginHistoryService.saveLoginHistory(loginHistory);
        }
    }

}
