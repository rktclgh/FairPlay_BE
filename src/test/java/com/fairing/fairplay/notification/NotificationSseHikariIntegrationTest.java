package com.fairing.fairplay.notification;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.notification.service.NotificationSseService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.jpa.open-in-view=false",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=1000"
})
@AutoConfigureMockMvc
class NotificationSseHikariIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private NotificationSseService notificationSseService;

    @Autowired
    private Environment environment;

    @MockBean
    private SessionService sessionService;

    @Test
    void authenticatedSseConnectionsDoNotHoldHikariConnections() throws Exception {
        when(sessionService.getSessionData(anyString())).thenAnswer(invocation -> {
            String sessionId = invocation.getArgument(0, String.class);
            long userId = Long.parseLong(sessionId.replace("session-", ""));
            return Map.of(
                    "userId", userId,
                    "email", "user" + userId + "@example.com",
                    "role", "COMMON",
                    "roleId", 2
            );
        });

        HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        try (Connection ignored = hikariDataSource.getConnection()) {
            // Force pool initialization so the MXBean reflects the active test pool.
        }

        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(2);
        assertThat(hikariDataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
        assertThat(hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();

        List<Long> userIds = new ArrayList<>();
        try {
            for (long userId = 1; userId <= 6; userId++) {
                userIds.add(userId);
                MvcResult result = mockMvc.perform(get("/api/notifications/stream")
                                .cookie(new Cookie("FAIRPLAY_SESSION", "session-" + userId))
                                .accept(MediaType.TEXT_EVENT_STREAM))
                        .andExpect(status().isOk())
                        .andExpect(request().asyncStarted())
                        .andReturn();

                assertThat(result.getRequest().isAsyncStarted()).isTrue();
            }

            assertThat(notificationSseService.getConnectedUserCount()).isEqualTo(6);
            assertThat(hikariDataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
        } finally {
            userIds.forEach(notificationSseService::removeEmitter);
        }

        assertThat(notificationSseService.getConnectedUserCount()).isZero();
        assertThat(hikariDataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
        assertThat(hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
    }
}
