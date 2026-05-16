package com.fairing.fairplay.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;

class SuperAdminServiceTest {

    @Test
    void readsTemplateContentWithoutRequiringFilesystemResource() throws Exception {
        var resource = new InMemoryHtmlResource("<html>ok</html>");

        String content = SuperAdminService.readTemplateContent(resource);

        assertThat(content).isEqualTo("<html>ok</html>");
    }

    private static class InMemoryHtmlResource extends AbstractResource {
        private final byte[] content;

        InMemoryHtmlResource(String content) {
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getDescription() {
            return "in-memory html resource";
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public File getFile() throws IOException {
            throw new FileNotFoundException("jar nested resource");
        }
    }
}
