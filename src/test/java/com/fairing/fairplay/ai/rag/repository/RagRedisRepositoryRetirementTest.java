package com.fairing.fairplay.ai.rag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class RagRedisRepositoryRetirementTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RagChunkRepository ragChunkRepository;

    @Test
    void redisRagRepositoryIsNotRegisteredAsProductionBean() {
        assertThat(applicationContext.containsBean("ragRedisRepository")).isFalse();
        assertThat(applicationContext.getBeanDefinitionNames()).doesNotContain("ragRedisRepository");
        assertThat(ClassUtils.isPresent(
            "com.fairing.fairplay.ai.rag.repository.RagRedisRepository",
            applicationContext.getClassLoader()))
            .isFalse();
    }

    @Test
    void activeRagChunkRepositoryUsesPgVector() {
        assertThat(ragChunkRepository).isInstanceOf(PgVectorRagRepository.class);
    }
}
