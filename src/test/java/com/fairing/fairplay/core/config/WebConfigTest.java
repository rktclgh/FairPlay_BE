package com.fairing.fairplay.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebConfigTest {

    @Test
    void allowedOriginsIncludesMiniserverDomainAndConfiguredUrls() {
        WebConfig webConfig = new WebConfig(
                "https://frontend.example.test",
                "https://api.example.test",
                "https://extra.example.test, https://fairplay.rktclgh.site");

        assertThat(webConfig.allowedOrigins())
                .contains(
                        "https://fairplay.rktclgh.site",
                        "https://frontend.example.test",
                        "https://api.example.test",
                        "https://extra.example.test")
                .doesNotHaveDuplicates();
    }
}
