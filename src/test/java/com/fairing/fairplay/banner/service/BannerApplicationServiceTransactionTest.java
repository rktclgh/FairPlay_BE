package com.fairing.fairplay.banner.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class BannerApplicationServiceTransactionTest {

    @Test
    void adminApplicationListKeepsSlotMappingInsideReadOnlyTransaction() throws NoSuchMethodException {
        Transactional transactional = BannerApplicationService.class
                .getDeclaredMethod("listAdminApplications", String.class, String.class, Pageable.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
