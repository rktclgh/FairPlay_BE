package com.fairing.fairplay.core.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class NginxMiniserverUploadDenyOrderTest {

    private static final Path NGINX_CONFIG = Path.of("deploy/nginx.miniserver.conf");

    @Test
    void uploadDenyLocationsPrecedeBroadUploadAlias() throws IOException {
        String config = Files.readString(NGINX_CONFIG);

        int privateDeny = config.indexOf("location = /uploads/private");
        int legacyTempDeny = config.indexOf("location = /uploads/uploads/temp");
        int broadUploadAlias = config.indexOf("location /uploads/ {");

        assertThat(privateDeny).isNotNegative();
        assertThat(legacyTempDeny).isNotNegative();
        assertThat(broadUploadAlias).isNotNegative();
        assertThat(privateDeny).isLessThan(broadUploadAlias);
        assertThat(legacyTempDeny).isLessThan(broadUploadAlias);
    }

    @Test
    void uploadDenyLocationsCoverPrivateTempAndLegacyTmpPaths() throws IOException {
        String config = Files.readString(NGINX_CONFIG);

        assertThat(config).contains(
                "location = /uploads/private",
                "location ^~ /uploads/private/",
                "location = /uploads/temp",
                "location ^~ /uploads/temp/",
                "location = /uploads/tmp",
                "location ^~ /uploads/tmp/",
                "location ~ ^/uploads/tmp[0-9]{4}-[0-9]{2}-[0-9]{2}(/|$)",
                "location = /uploads/uploads/temp",
                "location ^~ /uploads/uploads/temp/",
                "location = /uploads/uploads/tmp",
                "location ^~ /uploads/uploads/tmp/",
                "location ~ ^/uploads/uploads/tmp[0-9]{4}-[0-9]{2}-[0-9]{2}(/|$)"
        );

        assertThat(config).containsPattern("(?s)location = /uploads/private \\{\\s*return 404;\\s*}");
        assertThat(config).containsPattern("(?s)location \\^~ /uploads/uploads/temp/ \\{\\s*return 404;\\s*}");
        assertThat(config).containsPattern("(?s)location ~ \\^/uploads/uploads/tmp\\[0-9\\]\\{4}-\\[0-9\\]\\{2}-\\[0-9\\]\\{2}\\(/\\|\\$\\) \\{\\s*return 404;\\s*}");
    }

    @Test
    void datedTmpDenyPatternsDoNotCatchPermanentTmpNamedDirectories() {
        List<Pattern> datedTmpDenyPatterns = List.of(
                Pattern.compile("^/uploads/tmp[0-9]{4}-[0-9]{2}-[0-9]{2}(/|$)"),
                Pattern.compile("^/uploads/uploads/tmp[0-9]{4}-[0-9]{2}-[0-9]{2}(/|$)")
        );

        List<String> deniedPaths = List.of(
                "/uploads/tmp2026-05-18/source.png",
                "/uploads/uploads/tmp2026-05-18/source.png"
        );
        List<String> publicPaths = List.of(
                "/uploads/uploads/tmp-assets/source.png",
                "/uploads/uploads/temporary/source.png"
        );

        assertThat(deniedPaths).allSatisfy(path ->
                assertThat(datedTmpDenyPatterns).anySatisfy(pattern ->
                        assertThat(pattern.matcher(path).find()).isTrue()));
        assertThat(publicPaths).allSatisfy(path ->
                assertThat(datedTmpDenyPatterns).allSatisfy(pattern ->
                        assertThat(pattern.matcher(path).find()).isFalse()));
    }
}
