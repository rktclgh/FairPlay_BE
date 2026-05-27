package com.fairing.fairplay.notification.service;

import com.fairing.fairplay.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSseServiceTest {

    @Test
    void doesNotDependOnNotificationRepositoryForStreamConnections() {
        assertThat(NotificationSseService.class.getDeclaredFields())
                .filteredOn(field -> !field.isSynthetic())
                .extracting(field -> field.getType().getName())
                .doesNotContain(NotificationRepository.class.getName());
    }

    @Test
    void createEmitterRegistersEmitterForConnectedUser() {
        NotificationSseService sseService = new NotificationSseService();

        SseEmitter emitter = sseService.createEmitter(42L);

        assertThat(emitter).isNotNull();
        assertThat(sseService.getConnectedUserCount()).isEqualTo(1);
        sseService.removeEmitter(42L);
        assertThat(sseService.getConnectedUserCount()).isZero();
    }
}
