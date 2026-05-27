package com.fairing.fairplay.core.controller;

import com.fairing.fairplay.core.config.SecurityConfig;
import com.fairing.fairplay.core.service.AuthService;
import com.fairing.fairplay.core.service.RefreshTokenService;
import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.dto.UserResponseDto;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void sessionReturnsAnonymousWithoutCookie() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.user").doesNotExist());

        verify(sessionService, never()).getUserIdFromSession("missing");
    }

    @Test
    void sessionReturnsAnonymousForExpiredCookie() throws Exception {
        when(sessionService.getUserIdFromSession("expired")).thenReturn(null);

        mockMvc.perform(get("/api/auth/session")
                        .cookie(new MockCookie("FAIRPLAY_SESSION", "expired")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void sessionReturnsCurrentUserForValidCookie() throws Exception {
        UserResponseDto user = UserResponseDto.builder()
                .userId(10L)
                .email("admin@example.test")
                .name("Admin")
                .role("ADMIN")
                .build();

        when(sessionService.getUserIdFromSession("session-10")).thenReturn(10L);
        when(userService.getMyInfo(10L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/session")
                        .cookie(new MockCookie("FAIRPLAY_SESSION", "session-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.userId").value(10))
                .andExpect(jsonPath("$.user.email").value("admin@example.test"))
                .andExpect(jsonPath("$.user.name").value("Admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }
}
