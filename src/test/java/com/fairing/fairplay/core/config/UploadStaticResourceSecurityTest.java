package com.fairing.fairplay.core.config;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UploadStaticResourceSecurityTest.NoopController.class)
@Import({WebConfig.class, SecurityConfig.class})
class UploadStaticResourceSecurityTest {

    private static final Path UPLOAD_ROOT = createUploadRoot();

    static {
        System.setProperty("app.upload.path", UPLOAD_ROOT.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() throws Exception {
        writeFile("uploads/events/sample.txt", "public");
        writeFile("private/tmp/2026-05-18/source.txt", "private");
        writeFile("uploads/tmp2026-05-18/source.txt", "legacy tmp");
        writeFile("uploads/temp/2026-05-18/source.txt", "legacy temp");
        writeFile("uploads/tmp-assets/sample.txt", "tmp assets");
        writeFile("uploads/temporary/sample.txt", "temporary assets");
    }

    @Test
    void permanentUploadResourceIsPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/uploads/events/sample.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void privateTempResourceIsNotPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/private/tmp/2026-05-18/source.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legacyTmpResourceIsNotPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/uploads/tmp2026-05-18/source.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legacyTempResourceIsNotPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/uploads/temp/2026-05-18/source.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void permanentTmpAssetsResourceIsPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/uploads/tmp-assets/sample.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("tmp assets"));
    }

    @Test
    void permanentTemporaryResourceIsPubliclyServed() throws Exception {
        mockMvc.perform(get("/uploads/uploads/temporary/sample.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("temporary assets"));
    }

    private static Path createUploadRoot() {
        try {
            return Files.createTempDirectory("fairplay-upload-static-test");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void writeFile(String key, String content) throws IOException {
        Path file = UPLOAD_ROOT.resolve(key);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @RestController
    static class NoopController {
    }
}
