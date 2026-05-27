package com.fairing.fairplay.notification.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.notification.service.NotificationSseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSseControllerTest {

    @Mock
    NotificationSseService sseService;

    @Test
    void streamCreatesEmitterWithoutInitialNotificationLookup() {
        NotificationSseController controller = new NotificationSseController(sseService);
        CustomUserDetails userDetails = new CustomUserDetails(42L, "user@example.com", "COMMON", 2);
        SseEmitter expectedEmitter = new SseEmitter();
        when(sseService.createEmitter(42L)).thenReturn(expectedEmitter);

        SseEmitter actualEmitter = controller.stream(userDetails);

        assertThat(actualEmitter).isSameAs(expectedEmitter);
        verify(sseService).createEmitter(42L);
        verifyNoMoreInteractions(sseService);
    }

    @Test
    void streamReturnsCompletedEmitterWhenUserIsAnonymous() {
        NotificationSseController controller = new NotificationSseController(sseService);

        SseEmitter emitter = controller.stream(null);

        assertThat(emitter).isNotNull();
        verifyNoMoreInteractions(sseService);
    }
}
