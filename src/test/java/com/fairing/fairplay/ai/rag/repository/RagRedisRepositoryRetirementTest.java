package com.fairing.fairplay.ai.rag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class RagRedisRepositoryRetirementTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RagChunkRepository ragChunkRepository;

    @Test
    void redisRagRepositoryIsNotRegisteredAsProductionBean() {
        assertThat(applicationContext.containsBean("ragRedisRepository")).isFalse();
        ConfigurableApplicationContext configurableContext =
            (ConfigurableApplicationContext) applicationContext;
        assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
            .map(beanName -> configurableContext.getBeanFactory().getBeanDefinition(beanName))
            .map(BeanDefinition::getBeanClassName)
            .toList())
            .doesNotContain("com.fairing.fairplay.ai.rag.repository.RagRedisRepository");
    }

    @Test
    void activeRagChunkRepositoryUsesPgVector() {
        assertThat(ragChunkRepository).isInstanceOf(PgVectorRagRepository.class);
    }
}
