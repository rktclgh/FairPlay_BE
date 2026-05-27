package com.fairing.fairplay.history.aspect;

import com.fairing.fairplay.core.dto.LoginResponse;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.util.JwtTokenProvider;
import com.fairing.fairplay.history.entity.ChangeHistory;
import com.fairing.fairplay.history.etc.ChangeContent;
import com.fairing.fairplay.history.etc.ChangeEvent;
import com.fairing.fairplay.history.etc.ChangeTargetId;
import com.fairing.fairplay.history.etc.ChangeTemplate;
import com.fairing.fairplay.history.repository.ChangeHistoryRepository;
import com.fairing.fairplay.history.service.LoginHistoryService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessAspectTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private LoginHistoryService loginHistoryService;

  @Mock
  private ChangeHistoryRepository changeHistoryRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  private AccessAspect accessAspect;
  private CustomUserDetails principal;

  @BeforeEach
  void setUp() {
    accessAspect = new AccessAspect(userRepository, loginHistoryService,
        changeHistoryRepository, jwtTokenProvider);
    principal = new CustomUserDetails(10L, "admin@example.com", "ADMIN", 1);
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(principal, null));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void changeEventUsesAnnotatedTargetIdWithoutUserLookupForEventId() throws Throwable {
    when(userRepository.findById(10L)).thenReturn(Optional.of(new Users(10L)));
    Method method = SampleAuditedMethods.class.getDeclaredMethod(
        "changeEvent", CustomUserDetails.class, Long.class);

    Object result = accessAspect.aroundChangeEvent(joinPoint(method, principal, 777L));

    assertThat(result).isEqualTo("proceeded");
    ArgumentCaptor<ChangeHistory> captor = ArgumentCaptor.forClass(ChangeHistory.class);
    verify(changeHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getTargetId()).isEqualTo(777L);
    assertThat(captor.getValue().getTargetType()).isEqualTo("행사 정보 수정");
    assertThat(captor.getValue().getContent()).isEqualTo("행사 수정");
    verify(userRepository).findById(10L);
    verify(userRepository, never()).findById(777L);
  }

  @Test
  void changeTemplateUsesAnnotatedRequestBodyAsAuditContent() throws Throwable {
    when(userRepository.findById(10L)).thenReturn(Optional.of(new Users(10L)));
    Method method = SampleAuditedMethods.class.getDeclaredMethod(
        "saveTemplate", String.class, CustomUserDetails.class, String.class);

    Object result = accessAspect.aroundChangeTemplate(
        joinPoint(method, "terms", principal, "<html>updated</html>"));

    assertThat(result).isEqualTo("proceeded");
    ArgumentCaptor<ChangeHistory> captor = ArgumentCaptor.forClass(ChangeHistory.class);
    verify(changeHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getTargetId()).isNull();
    assertThat(captor.getValue().getTargetType()).isEqualTo("템플릿 수정");
    assertThat(captor.getValue().getContent()).isEqualTo("<html>updated</html>");
  }

  @Test
  void changeEventDoesNotWriteAuditHistoryWhenProceedFails() throws Throwable {
    when(userRepository.findById(10L)).thenReturn(Optional.of(new Users(10L)));
    Method method = SampleAuditedMethods.class.getDeclaredMethod(
        "changeEvent", CustomUserDetails.class, Long.class);
    ProceedingJoinPoint joinPoint = joinPointThrowing(method, new IllegalStateException("denied"),
        principal, 777L);

    assertThatThrownBy(() -> accessAspect.aroundChangeEvent(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("denied");

    verify(changeHistoryRepository, never()).save(org.mockito.Mockito.any());
  }

  @Test
  void changeEventFailsClosedWhenTargetIdBindingIsMissing() throws Throwable {
    Method method = SampleAuditedMethods.class.getDeclaredMethod(
        "changeEventWithoutTargetBinding", CustomUserDetails.class, Long.class);
    ProceedingJoinPoint joinPoint = joinPointWithoutProceedStub(method, principal, 777L);

    assertThatThrownBy(() -> accessAspect.aroundChangeEvent(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targetId 바인딩 누락");

    verify(joinPoint, never()).proceed();
    verifyNoInteractions(changeHistoryRepository);
  }

  @Test
  void changeTemplateFailsClosedWhenContentBindingIsMissing() throws Throwable {
    Method method = SampleAuditedMethods.class.getDeclaredMethod(
        "saveTemplateWithoutContentBinding", String.class, CustomUserDetails.class, String.class);
    ProceedingJoinPoint joinPoint = joinPointWithoutProceedStub(method, "terms", principal,
        "<html>updated</html>");

    assertThatThrownBy(() -> accessAspect.aroundChangeTemplate(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("content 바인딩 누락");

    verify(joinPoint, never()).proceed();
    verifyNoInteractions(changeHistoryRepository);
  }

  private ProceedingJoinPoint joinPoint(Method method, Object... args) throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getArgs()).thenReturn(args);
    when(joinPoint.proceed()).thenReturn("proceeded");
    return joinPoint;
  }

  private ProceedingJoinPoint joinPointThrowing(Method method, Throwable throwable, Object... args)
      throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getArgs()).thenReturn(args);
    when(joinPoint.proceed()).thenThrow(throwable);
    return joinPoint;
  }

  private ProceedingJoinPoint joinPointWithoutProceedStub(Method method, Object... args) {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getArgs()).thenReturn(args);
    return joinPoint;
  }

  private static class SampleAuditedMethods {
    @ChangeEvent("행사 수정")
    @SuppressWarnings("unused")
    Object changeEvent(CustomUserDetails userDetails, @ChangeTargetId Long eventId) {
      return null;
    }

    @ChangeTemplate("템플릿 저장")
    @SuppressWarnings("unused")
    Object saveTemplate(String name, CustomUserDetails userDetails, @ChangeContent String content) {
      return null;
    }

    @ChangeEvent("행사 수정")
    @SuppressWarnings("unused")
    Object changeEventWithoutTargetBinding(CustomUserDetails userDetails, Long eventId) {
      return null;
    }

    @ChangeTemplate("템플릿 저장")
    @SuppressWarnings("unused")
    Object saveTemplateWithoutContentBinding(String name, CustomUserDetails userDetails, String content) {
      return null;
    }
  }
}
