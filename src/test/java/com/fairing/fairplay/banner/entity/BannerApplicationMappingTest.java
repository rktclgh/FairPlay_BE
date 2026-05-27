package com.fairing.fairplay.banner.entity;

import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BannerApplicationMappingTest {

    @Test
    void adminCommentIsPlainTextNotPostgresOidLob() throws NoSuchFieldException {
        assertThat(BannerApplication.class.getDeclaredField("adminComment").isAnnotationPresent(Lob.class))
                .isFalse();
    }
}
