package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothExperienceReservationRequestDto;
import com.fairing.fairplay.booth.dto.BoothExperienceReservationResponseDto;
import com.fairing.fairplay.booth.service.BoothExperienceService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import java.util.Arrays;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothExperienceControllerPrincipalTest {

  @Mock
  private BoothExperienceService boothExperienceService;

  private BoothExperienceController controller;

  @BeforeEach
  void setUp() {
    controller = new BoothExperienceController(boothExperienceService);
  }

  @Test
  void createReservationPassesAuthenticationPrincipalToService() {
    CustomUserDetails principal = mock(CustomUserDetails.class);
    BoothExperienceReservationRequestDto request = BoothExperienceReservationRequestDto.builder()
        .notes("reserve")
        .build();
    BoothExperienceReservationResponseDto response = BoothExperienceReservationResponseDto.builder()
        .reservationId(7L)
        .userId(300L)
        .build();
    when(boothExperienceService.createReservation(1L, principal, request)).thenReturn(response);

    var result = controller.createReservation(1L, request, principal);

    assertThat(result.getBody()).isSameAs(response);
    verify(boothExperienceService).createReservation(1L, principal, request);
  }

  @Test
  void createReservationDoesNotAcceptUserIdRequestParam() throws NoSuchMethodException {
    Method method = BoothExperienceController.class.getMethod(
        "createReservation",
        Long.class,
        BoothExperienceReservationRequestDto.class,
        CustomUserDetails.class
    );

    assertThat(method.getParameters())
        .allSatisfy(parameter -> assertThat(parameter.isAnnotationPresent(RequestParam.class)).isFalse());
    assertThat(method.getParameters()[2].isAnnotationPresent(AuthenticationPrincipal.class)).isTrue();
  }

  @Test
  void managementPreAuthorizeExpressionsAllowAdmin() {
    assertThat(Arrays.stream(BoothExperienceController.class.getDeclaredMethods())
        .map(method -> method.getAnnotation(PreAuthorize.class))
        .filter(annotation -> annotation != null
            && annotation.value().contains("EVENT_MANAGER")
            && annotation.value().contains("BOOTH_MANAGER"))
        .map(PreAuthorize::value))
        .allSatisfy(expression -> assertThat(expression).contains("ADMIN"));
  }
}
