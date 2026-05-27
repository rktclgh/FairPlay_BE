package com.fairing.fairplay.core.controller;

import com.fairing.fairplay.core.config.SecurityConfig;
import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpaForwardController.class)
@Import(SecurityConfig.class)
class SpaForwardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void ticketReservationFrontendRouteForwardsToSpaWithoutSession() throws Exception {
        mockMvc.perform(get("/ticket-reservation/52").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void supportFrontendRoutesForwardToSpaWithoutSession() throws Exception {
        mockMvc.perform(get("/support/notices").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/support/faq").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void legalFrontendRoutesForwardToSpaWithoutSession() throws Exception {
        mockMvc.perform(get("/legal/privacy").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void boothPaymentFrontendRouteForwardsToSpaWithoutSession() throws Exception {
        mockMvc.perform(get("/booth/payment").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void ticketReservationApiLikeRequestStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reservations/mypage").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
