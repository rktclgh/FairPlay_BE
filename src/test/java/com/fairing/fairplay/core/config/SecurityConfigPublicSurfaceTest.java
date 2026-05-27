package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.controller.FileUploadController;
import com.fairing.fairplay.core.security.SessionAuthenticationFilter;
import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileUploadController.class)
@Import(SecurityConfig.class)
class SecurityConfigPublicSurfaceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocalFileService localFileService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void uploadDownloadRequiresAuthenticationWithoutSession() throws Exception {
        mockMvc.perform(get("/api/uploads/download").param("key", "uploads/events/banner.txt"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(localFileService);
    }

    @Test
    void tempUploadRequiresAuthenticationWithoutSession() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.txt",
                "text/plain",
                "banner".getBytes());

        mockMvc.perform(multipart("/api/uploads/temp").file(file))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(localFileService);
    }

    @Test
    void paymentCompleteRequiresAuthenticationWithoutSession() throws Exception {
        mockMvc.perform(post("/api/payments/complete")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realtimeQrAndWaitingStreamsRequireAuthenticationWithoutSession() throws Exception {
        mockMvc.perform(get("/api/qr-tickets/1/stream"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/booth-experiences/waiting/stream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyPublicSockJsSurfacesRequireAuthenticationWithoutSession() throws Exception {
        mockMvc.perform(get("/ws/qr-sockjs/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/ws/waiting-sockjs/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/ws/booth-sockjs/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bannerPaymentCompleteRemainsPublicWithoutSession() throws Exception {
        mockMvc.perform(post("/api/banners/payment/complete")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(401, 403));
    }

    @Test
    void availableBoothExperienceListRemainsPublicWithoutSession() throws Exception {
        mockMvc.perform(get("/api/booth-experiences/available"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(401, 403));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sessionFilterDoesNotClassifyUploadApiAsPublicPath() {
        List<String> publicPaths = (List<String>) ReflectionTestUtils.getField(
                SessionAuthenticationFilter.class,
                "PUBLIC_PATHS");

        assertThat(publicPaths).doesNotContain("/api/uploads/");
        assertThat(publicPaths).doesNotContain("/ws/");
    }
}
