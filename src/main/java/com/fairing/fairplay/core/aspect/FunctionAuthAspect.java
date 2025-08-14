package com.fairing.fairplay.core.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;

@Aspect
@Component
public class FunctionAuthAspect {

    private final AccountLevelRepository accountLevelRepository;

    public FunctionAuthAspect(AccountLevelRepository accountLevelRepository) {
        this.accountLevelRepository = accountLevelRepository;
    }
    // @Around("@annotation(com.fairing.fairplay.core.etc.FunctionAuth)")
    // public Object checkFunctionAuth(ProceedingJoinPoint joinPoint) throws
    // Throwable {
    // MethodSignature sig = (MethodSignature) joinPoint.getSignature();
    // Method method = sig.getMethod();

    // FunctionAuth annotaion = method.getAnnotation(FunctionAuth.class);
    // String methodName = annotaion.value();
    // FunctionLevelEnum functionLevelEnum =
    // FunctionLevelEnum.fromFunctionName(methodName);
    // Long userId = 1L; // getCurrentUserId로 수정 예정 - 테스트를위해 하드코딩
    // BigInteger accountLevel = accountLevelRepository.findById(userId).map(level
    // -> level.getLevel()).orElse(null)
    // .toBigInteger();
    // if
    // (!(functionLevelEnum.getBit().and(accountLevel).equals(functionLevelEnum.getBit())))
    // {
    // throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
    // }

    // return joinPoint.proceed();

    // }

    // private Long getCurrentUserId() {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // Object principal = authentication.getPrincipal();
    // if (principal instanceof CustomUserDetails) {
    // return ((CustomUserDetails) principal).getUserId();
    // }

    // return null; // 인증되지 않은 경우 null 반환
    // }
}
