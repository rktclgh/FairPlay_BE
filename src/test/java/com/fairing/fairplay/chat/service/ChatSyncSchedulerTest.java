package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.scheduler.PresenceCleanupScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSyncSchedulerTest {

    @Test
    void presenceCleanupSchedulingIsOwnedByPresenceCleanupSchedulerOnly() throws NoSuchMethodException {
        boolean chatSyncSchedulesPresenceCleanup = Arrays.stream(ChatSyncScheduler.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .map(Method::getName)
                .anyMatch("cleanupExpiredPresence"::equals);

        Method presenceCleanup = PresenceCleanupScheduler.class.getDeclaredMethod("cleanupExpiredPresence");

        assertThat(chatSyncSchedulesPresenceCleanup).isFalse();
        assertThat(presenceCleanup.isAnnotationPresent(Scheduled.class)).isTrue();
    }
}
