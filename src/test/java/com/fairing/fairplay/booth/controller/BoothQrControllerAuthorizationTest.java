package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.core.security.CustomUserDetails;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class BoothQrControllerAuthorizationTest {

  @Test
  void boothEntryPreAuthorizeExpressionAllowsAdmin() throws NoSuchMethodException {
    Method method = BoothQrController.class.getMethod(
        "boothEntry",
        BoothEntryRequestDto.class,
        CustomUserDetails.class
    );

    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

    assertThat(preAuthorize).isNotNull();
    assertThat(preAuthorize.value()).contains("ADMIN");
  }
}
